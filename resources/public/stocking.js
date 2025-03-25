
/* toto.js */

import { clearCache, visit } from './turbo-7.1.0.js';

function elemOptional(klass) {
    var elements = document.getElementsByClassName(klass);

    if (elements.length > 0) {
        return elements[0];
    }

    return null;
}

function elem(klass) {
    var element =  elemOptional(klass);

    if(!element) {
        console.error('Expected missing element with class: ' + klass);
    }

    return element;
}

function elemOptionalById(id) {
    return document.getElementById(id);
}

function elemById(id) {
    var element = elemOptionalById(id);

    if(!element) {
        console.error('Expected missing element with id: ' + id);
    }

    return element;
}

function elemBySelector(selector) {
    var elements = document.querySelectorAll(selector);

    if (elements.length == 0) {
        console.error('Expected missing element with selector: ' + selector);
    } else if (elements.length > 0) {
        if (elements.length > 1) {
            console.error('Warning: more than one element with selector: ' + selector);
        }

        return elements[0];
    }

    return null;
}

function foreach_elem(selector, fn) {
    Array.prototype.forEach.call(document.querySelectorAll(selector), function(el, i) { fn(el); });
}

function doubleTapFilter(onSingleTap, onDoubleTap) {
    var DOUBLETAP_TIME_MSEC = 300;

    var doubletapTimer_ = null;

    function doubletapTimeout() {
        if (onSingleTap) {
            onSingleTap();
        }

        doubletapTimer_ = null;
    }

    return function() {
        if (doubletapTimer_) {
            clearTimeout(doubletapTimer_);
            doubletapTimer_ = null;
            if(onDoubleTap) {
                onDoubleTap();
            }
        } else {
            doubletapTimer_ = setTimeout(doubletapTimeout, DOUBLETAP_TIME_MSEC);
        }
    };
}

function visitPage(target)
{
    saveScrolls();
    clearCache();
    visit(target);
}

function refreshPage()
{
    visitPage(location);
}

function server(url, options) {
    return fetch(url, {
        credentials: 'include',
        ...options
    }).then((response) => {
        if (response.redirected) {
            window.location.replace(response.url);
            visit(response.url);
        }

        return response;
    });
}

function doPost(baseUrl, args, nextUrl) {
    var queryArgs = [];

    for(const argName in args) {
        queryArgs.push((argName + '=' + args[argName]));
    }

    var url = baseUrl;

    if (queryArgs.length) {
        url += ('?' + queryArgs.join('&'));
    }

    function doRefresh() {
        if (nextUrl) {
            visitPage(nextUrl);
        } else {
            refreshPage();
        }
    }

    server(url, {
        method: 'POST',
    }).then(doRefresh);
}

function makeDropTarget(el, onDrop) {
    var dragCount = 0;

    function updateTarget() {
        if (dragCount > 0) {
            el.classList.add('drop-hover');
        } else {
            el.classList.remove('drop-hover');
        }
    }

    el.ondragenter = function(ev) {
        ev.preventDefault();

        dragCount++;
        updateTarget();
    };

    el.ondragleave = function(ev) {
        ev.preventDefault();

        dragCount--;
        updateTarget();
    };

    el.ondragover = function(ev) {
        ev.preventDefault();
    };

    el.ondrop = function(ev) {
        ev.preventDefault();

        dragCount = 0;
        updateTarget();

        onDrop(ev);
    };

    el.ondragend = function(ev) {
        ev.preventDefault();

        dragCount = 0;
        updateTarget();
    };
}

function dismissQueryIfPresent() {
    /** This dismisses either a query or a model, given that
     *  modals are requested via the query string. */
    if (window.location.search !== '') {
        visitPage(window.location.pathname);
    }
}

function dismissModalIfPresent() {
    var modal = elemOptional('modal');

    if (!modal) {
        return;
    }

    var nextUrl = modal.getAttribute('data-escape-url');

    if (nextUrl) {
        visitPage(nextUrl);
    }
}

function checkShortcutBindings(event, elem) {
    var shortcuts = elem.querySelectorAll('*[data-shortcut-key]');
    for(let shortcut of shortcuts) {
        if (event.key === shortcut.getAttribute('data-shortcut-key')) {
            event.preventDefault();
            shortcut.click();
            return true;
        }
    }

    return false;
}

function checkModalShortcutBindings(event) {
    var modal = elemOptional('modal');

    return modal && checkShortcutBindings(event, modal);
}

function checkDocumentShortcutBindings(event) {
    return checkShortcutBindings(event, document);
}

function onDocumentKeydown(event) {
    if (event.keyCode == 27) {
        dismissQueryIfPresent();
    } else if (!checkModalShortcutBindings(event)) {
        checkDocumentShortcutBindings(event);
    }
}

function onDocumentClick(event) {
    var modalBackground = elemOptional('dialog-background');

    if (modalBackground && (event.target === modalBackground)) {
        dismissModalIfPresent();
    }
}

//////// todo list

function doSetToggle(el) {
    visitPage(location.pathname + "?" + el.id  + "=" + el.checked);
}

function doUpdateItem(itemId, newDescription, action, thenUrl) {
    var formData = new FormData();

    formData.append("description", newDescription);
    formData.append("action", action);

    server("/item/" + itemId, {
        body: formData,
        method: "POST",
    }).then(() => visitPage(thenUrl));
}

function doSetItemOrdinal(itemId, newItemOrdinal) {
    var formData = new FormData();

    formData.append("new-ordinal", newItemOrdinal);

    server("/item/" + itemId + "/ordinal", {
        body: formData,
        method: "POST",
    }).then(refreshPage);
}

function setupItemDragging() {
    foreach_elem(".drag-handle", function(el) {
        el.setAttribute('draggable', true);

        el.ondragstart = function(ev) {
            var itemId = el.getAttribute('itemid');

            ev.dataTransfer.dropEffect = 'move';
            ev.dataTransfer.setData('text/plain', itemId);

            var rowElem = elemById("item_row_" + itemId);

            ev.dataTransfer.setDragImage(rowElem, 0, 0);
        };
    });

    foreach_elem('.order-drop-target', function(el) {
        makeDropTarget(el, function(ev) {
            ev.preventDefault();

            var itemId = ev.dataTransfer.getData("text/plain");

            var newListId = ev.currentTarget.getAttribute('listid');
            var newItemOrdinal = ev.currentTarget.getAttribute('ordinal');

            doSetItemOrdinal(itemId, newItemOrdinal);
        });
    });
}

function initTodoList() {
    setupItemDragging();
};

function checkVerifyFields(field1, field2, message) {
    function doCheck() {
        var pwd1 = elemById(field1).value.trim();
        var pwd2 = elemById(field2).value.trim();

        var errDiv = elemById("error");

        if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
            errDiv.innerHTML = message;
        else
            errDiv.innerHTML = "";
    }

    elemById(field1).onkeyup = doCheck;
    elemById(field1).onchange = doCheck;
    elemById(field2).onkeyup = doCheck;
    elemById(field2).onchange = doCheck;
}

function initNewUser() {
    checkVerifyFields("password", "password-2", "Passwords do not match.");
    checkVerifyFields("email-addr", "email-addr-2", "E-mail addresses do not match.");
};

function onItemDescriptionKeydown(event) {
    if (event.metaKey && event.keyCode == 13) {
        event.preventDefault();
        event.stopPropagation();
        event.target.form.submit();
    }
}

function initAddStockingModal(modal) {
    function doCheck() {
        var toName = elemById("to-name").value.trim();
        var fromName = elemById("from-name").value.trim();
        var listDescription = elemById("list-description").value.trim();

        elemById("submit-list").disabled = !toName || !fromName || !listDescription;
    }

    elemById("to-name").onkeyup = doCheck;
    elemById("to-name").onchange = doCheck;
    elemById("from-name").onkeyup = doCheck;
    elemById("from-name").onchange = doCheck;
    elemById("list-description").onkeyup = doCheck;
    elemById("list-description").onchange = doCheck;
}


function pasteText(textEl, newText) {
    var startPos = textEl.selectionStart;
    var endPos = textEl.selectionEnd;

    textEl.value = textEl.value.substring(0, startPos)
        + newText
        + textEl.value.substring(endPos, textEl.value.length);

    textEl.selectionStart = startPos + newText.length;
    textEl.selectionEnd = startPos + newText.length;
}

function onDescriptionPaste(event) {
    const itemDescElem = elemById("item-description");
    const item = event.clipboardData.items[0];

    if (item.kind !== 'file') {
        return;
    }

    const type = item.type;

    var reader = new FileReader();
    reader.onload = function(event){
        server("/image", {
            headers: {
                'Content-Type': type
            },
            body: event.target.result,
            method: "POST",
        }).then(resp => resp.json())
            .then(resp => {
                pasteText(itemDescElem, "<img src=\"" + resp["url"] + "\"/>");
            });
    };
    reader.readAsArrayBuffer(item.getAsFile());
};

function initItemModal(modal) {
    function doCheck() {
        var itemTitle = elemById("item-title").value.trim();
        var itemDescription = elemById("item-description").value.trim();

        elemById("submit-item").disabled = !itemTitle || !itemDescription;
    }

    var descriptionTimeout;

    function doCheckDescription() {
        let desc = elemById("item-description").value.trim();
        let lengthIndicator = elemById("length-indicator");

        lengthIndicator.textContent = desc.length;

        if (!descriptionTimeout) {
            descriptionTimeout = setTimeout(function() {
                descriptionTimeout = null;

                let desc = elemById("item-description").value.trim();

                server("/format-item-body", {
                    body: desc,
                    method: "POST",
                }).then((response) => {
                    response.text().then(function(html) {
                        elemById("item-preview").innerHTML = html;
                    });
                });

            }, 1000);
        }

        doCheck();
    }

    const itemTitleElem = elemById("item-title");
    const itemDescElem = elemById("item-description");

    itemTitleElem.onkeyup = doCheck;
    itemTitleElem.onchange = doCheck;

    itemDescElem.onkeyup = doCheckDescription;
    itemDescElem.onchange = doCheckDescription;

    itemDescElem.onpaste = onDescriptionPaste;
}

const initFns = {
    'todo-list': initTodoList,
    'init-new-user': initNewUser
};

const initModalFns = {
    'item-modal': initItemModal,
    'add-stocking-modal': initAddStockingModal
};

function runPageInitScript() {
    let body = document.getElementsByTagName('body')[0];

    let initFn = initFns[body.getAttribute('data-class')];

    if (initFn) {
        initFn();
    }

    const modals = document.querySelectorAll('[data-init-modal]');

    for(const modal of modals) {
        const modalFn = initModalFns[modal.getAttribute('data-init-modal')];

        if(modalFn) {
            modalFn(modal);
        }
    }
};


window.savedScrolls = {};

function saveScrolls() {
    foreach_elem("[data-preserve-scroll=true]", function(elem) {
        if (elem.id === '') {
            console.warn("Cannot preserve scroll on element without an id");
        }

        window.savedScrolls[elem.id] = elem.scrollTop;
    });
}

function restoreScrollAndAutofocusStates(_) {
    const autofocusedElements = document.querySelectorAll('input[autofocus]');

    if (autofocusedElements.length) {
        autofocusedElements[0].focus();
    }

    for(const id in window.savedScrolls) {
        const elem = elemOptionalById(id);

        if (elem) {
            elem.scrollTop = window.savedScrolls[elem.id];
        }
    }
};

// Copy

function doCopy(event) {
    let copyButtonEl = event.target;

    let copyText = copyButtonEl.parentElement.querySelector('input').value;

    navigator.clipboard.writeText(copyText)
        .then(() => {
            copyButtonEl.textContent = "Copied";
        });
}

// startup

document.addEventListener("turbo:load", runPageInitScript);
document.addEventListener("turbo:before-visit", saveScrolls);
document.addEventListener("turbo:render", restoreScrollAndAutofocusStates);
document.addEventListener("keydown", onDocumentKeydown);
document.addEventListener("click", onDocumentClick);

// exports

window._toto = {
    doPost,
    doCopy,
    doSetToggle,
    onItemDescriptionKeydown
};
