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

var sidebarVisible = false;

function onToggleSidebar(evt) {
    evt.preventDefault();

    var menu = elemOptional('sidebar');

    if (!menu || menu.contains(evt.target)) {
        return;
    }

    doToggleSidebar(evt);
}

function doToggleSidebar(evt) {
    evt.preventDefault();

    sidebarVisible = !sidebarVisible;

    updateSidebarVisibility(true);
}

function updateSidebarVisibility(animated) {
    const menu = elemOptional('sidebar');

    if (!menu) {
        return;
    }

    menu.classList.toggle('animated', animated);
    menu.classList.toggle('menu-visible', sidebarVisible);

    if(sidebarVisible) {
        document.addEventListener("mousedown", onToggleSidebar);
    } else {
        document.removeEventListener("mousedown", onToggleSidebar);
    }
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

function setupSidebar() {
    var toggleMenu = elemOptional('toggle-menu');

    if (toggleMenu) {
        toggleMenu.ontouchstart =  onToggleSidebar;
        toggleMenu.onclick =  onToggleSidebar;
    }

    var closeMenu = elemOptional('close-menu');

    if (closeMenu) {
        closeMenu.ontouchstart = doToggleSidebar;
        closeMenu.onclick = doToggleSidebar;
    }
};

//////// todo list

function setupEditableItems() {
    foreach_elem('.toplevel-list .item-row.display', function(el) {
        el.onclick = doubleTapFilter(null, function(obj) {
            visitPage(el.getAttribute('edit-href'));
        });
    });
}

function doPost(baseUrl, args, nextUrl) {
    var queryArgs = [];

    for(const argName in args) {
        queryArgs.push((argName + '=' + args[argName]));
    }

    var url = baseUrl;

    // TODO: Properly handle case where baseUrl has args already
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

    fetch(url, {
        method: 'POST',
        credentials: 'include'
    }).then(doRefresh);
}

function doUpdateItem(itemId, newDescription, thenUrl) {
    var formData = new FormData();

    formData.append("description", newDescription);

    fetch("/item/" + itemId, {
        body: formData,
        method: "POST",
        credentials: 'include'
    }).then(() => visitPage(thenUrl));
}

function doMoveItem(itemId, newListId) {
    var formData = new FormData();

    formData.append("target-item", itemId);
    formData.append("target-list", newListId);

    fetch("/item/" + itemId + "/list", {
        body: formData,
        method: "POST",
        credentials: 'include'
    }).then(refreshPage);
}

function doSetItemOrdinal(itemId, listId, newItemOrdinal, newItemPriority) {
    var formData = new FormData();

    formData.append("target-item", itemId);
    formData.append("target-list", listId);
    formData.append("new-ordinal", newItemOrdinal);
    formData.append("new-priority", newItemPriority);

    fetch("/item/" + itemId + "/ordinal", {
        body: formData,
        method: "POST",
        credentials: 'include'
    }).then(refreshPage);
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

function setupItemDragging() {
    foreach_elem(".item-drag-handle", function(el) {
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
            var newItemPriority = ev.currentTarget.getAttribute('priority');

            doSetItemOrdinal(itemId, newListId, newItemOrdinal, newItemPriority);
        });
    });

    foreach_elem('.list-list .list-drop-target', function(el) {
        makeDropTarget(el, function(ev) {
            ev.preventDefault();

            var itemId = ev.dataTransfer.getData("text/plain");
            var newListId = ev.currentTarget.getAttribute('listid');

            doMoveItem(itemId, newListId);
        });
    });
}


function submitHighPriority() {
    var form = elemBySelector('.new-item-form');
    var priorityField = elemBySelector('.new-item-form #item-priority');

    priorityField.value = "1";
    form.submit();
}

function selectNavigate(id, direction) {
    var s = elemById(id);

    if (s.type !== 'select-one') {
        return;
    }

    var newIndex = s.selectedIndex + direction;

    if (newIndex < 0) {
        newIndex = s.options.length - 1;
    } else if (newIndex >= s.options.length) {
        newIndex = 0;
    }

    s.selectedIndex = newIndex;
}

function onNewItemInputKeydown(event) {

    if (event.ctrlKey) {
        if (event.keyCode == 13) {
            event.preventDefault();
            event.stopPropagation();
            submitHighPriority();
        }

    } else if (event.shiftKey) {
        if (event.keyCode == 38) {
            event.preventDefault();
            event.stopPropagation();
            selectNavigate('item-list-id', -1);
        } else if (event.keyCode == 40) {
            event.preventDefault();
            event.stopPropagation();
            selectNavigate('item-list-id', 1);
        }
    }
}

function onItemEditKeydown(event) {
    var input = event.target;

    if (event.keyCode == 13) {
        event.preventDefault();
        event.stopPropagation();

        doUpdateItem(input.getAttribute('item-id'),
                     input.value,
                     input.getAttribute('view-href'));
    } else if (event.keyCode == 27) {
        visitPage(input.getAttribute('view-href'));
    }
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

function checkModalShortcutBindings(event) {
    var modal = elemOptional('modal');

    if (!modal) {
        return;
    }

    var shortcuts = modal.querySelectorAll('span[data-shortcut-key]');

    for(let shortcut of shortcuts) {
        if (event.key === shortcut.getAttribute('data-shortcut-key')) {
            event.preventDefault();
            shortcut.onclick();
        }
    }
}

function onDocumentKeydown(event) {
    if (event.keyCode == 27) {
        dismissQueryIfPresent();
    } else {
        checkModalShortcutBindings(event);
    }
}

function onDocumentClick(event) {
    var modalBackground = elemOptional('dialog-background');

    if (modalBackground && (event.target === modalBackground)) {
        dismissModalIfPresent();
    }
}

function initTodoList() {
    setupEditableItems();
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

const initFns = {
    'todo-list': initTodoList,
    'init-new-user': initNewUser
};

document.addEventListener("turbo:load", function() {
    let body = document.getElementsByTagName('body')[0];

    let initFn = initFns[body.getAttribute('data-class')];

    if (initFn) {
        initFn();
    }
});


window.savedScrolls = {};

function saveScrolls() {
    foreach_elem("[data-preserve-scroll=true]", function(elem) {
        if (elem.id === '') {
            console.warn("Cannot preserve scroll on element without an id");
        }

        window.savedScrolls[elem.id] = elem.scrollTop;
    });
}

function restoreScrolls() {
    for(const id in window.savedScrolls) {
        const elem = elemOptionalById(id);

        if (elem) {
            elem.scrollTop = window.savedScrolls[elem.id];
        }
    }
}

function updateScrollState(elem) {
    elem.classList.toggle('scrolled', elem.scrollTop > 0);
    elem.classList.toggle('additional-content', elem.scrollHeight - elem.scrollTop > elem.clientHeight);
}

function setupScrollListeners() {
    foreach_elem(".scroll-column .scrollable", function(elem) {
        updateScrollState(elem);

        elem.addEventListener('scroll', (event) => {
            updateScrollState(event.target);
        });
    });
}

document.addEventListener("turbo:before-render", function(event) {
    saveScrolls();
});

function isDisplayingAllLists() {
    let params = (new URL(document.location)).searchParams;
    let minListPriority = params.get("min-list-priority");

    return minListPriority && minListPriority == "-1";
}

var prevDisplaysAllLists = isDisplayingAllLists();

function displaySidebarOnChangingListQuery() {
    const displaysAllLists = isDisplayingAllLists();

    sidebarVisible = displaysAllLists != prevDisplaysAllLists;
    prevDisplaysAllLists = displaysAllLists;
}

document.addEventListener("turbo:render", function(event) {
    setupSidebar();

    const autofocusedElements = document.querySelectorAll('input[autofocus]');

    if (autofocusedElements.length) {
        autofocusedElements[0].focus();
    }

    displaySidebarOnChangingListQuery();

    restoreScrolls();
    updateSidebarVisibility(false);
    setupScrollListeners();
});

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

document.addEventListener('DOMContentLoaded', function() {
    setupSidebar();
    setupScrollListeners();
}, false);

document.addEventListener("keydown", onDocumentKeydown);
document.addEventListener("click", onDocumentClick);

window._toto = {
    doPost,
    doCopy,
    onItemEditKeydown,
    onNewItemInputKeydown,
    submitHighPriority
};
