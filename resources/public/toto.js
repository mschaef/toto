
/* toto.js */

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

sidebarVisible = false;

function onToggleSidebar(evt) {
    evt.preventDefault();

    var menu = elem('sidebar');

    if (menu.contains(evt.target)) {
        return;
    }

    doToggleSidebar(evt);
}

function doToggleSidebar(evt) {
    evt.preventDefault();

    var menu = elem('sidebar');

    sidebarVisible = !sidebarVisible;

    menu.classList.toggle('menu-visible', sidebarVisible);

    if(sidebarVisible) {
        document.addEventListener("mousedown", onToggleSidebar);
    } else {
        document.removeEventListener("mousedown", onToggleSidebar);
    }
}

function refreshPage()
{
    saveScrolls();
    Turbolinks.clearCache();
    Turbolinks.visit(location);
}

function beginUserAdd(listId)
{
  var formMarkup = "";

  formMarkup += "<input class=\"full-width simple-border\" id=\"share-with-email\" name=\"share-with-email\" type=\"text\" />";

  elem('new-user').outerHTML = formMarkup;
  elem('share-with-email').focus();
}

function beginItemEdit(itemId)
{
  var itemDesc = elemById('item_desc_' + itemId).textContent;

  var formMarkup = "";
  formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
  formMarkup += "<button type=\"submit\" class=\"item-button\"><i class=\"fa fa-trash-o icon-black\"></i></button>";
  formMarkup += "</form>";

  elemById('item_control_' + itemId).innerHTML = formMarkup;

  formMarkup = "";
  formMarkup += "<form id=\"iedit_" + itemId + "\"  class=\"embedded\" action=\"/item/" + itemId + "\" method=\"POST\">";
  formMarkup += "<input class=\"full-width simple-border\" id=\"iedit_" + itemId + "_description\" name=\"description\" type=\"text\"/>";
  formMarkup += "</form>";

  elemById('item_' + itemId).outerHTML = formMarkup;
  elemById("iedit_" + itemId + "_description").value = itemDesc;
  elemById("iedit_" + itemId + "_description").focus();
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

function checkPasswords()
{
    var pwd1 = elemById("password1").value.trim();
    var pwd2 = elemById("password2").value.trim();

    var errDiv = elemById("error");

    if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
        errDiv.innerHTML = "Passwords do not match";
    else
        errDiv.innerHTML = "";
}

//////// todo list

function setupEditableItems() {
    foreach_elem('.toplevel-list .item-row .item-description', function(el) {
        el.onclick = doubleTapFilter(null, function(obj) {
            beginItemEdit(el.getAttribute('itemid'));
        });
    });
}

function doPost(baseUrl, args) {
    var queryArgs = [];

    for(const argName in args) {
        queryArgs.push((argName + '=' + args[argName]));
    }

    var url = baseUrl;

    if (queryArgs.length) {
        url += ('?' + queryArgs.join('&'));
    }

    fetch(url, {
        method: 'POST'
    }).then(refreshPage);
}

function deleteItem(itemId) {
    updateItem(itemId, 'delete');
}

function doMoveItem(itemId, newListId) {
    var formData = new FormData();

    formData.append("target-item", itemId);
    formData.append("target-list", newListId);

    fetch("/item-list", {
        body: formData,
        method: "post"
    }).then(refreshPage);
}

function doSetItemOrder(itemId, newItemOrdinal, newItemPriority) {
    var formData = new FormData();

    formData.append("target-item", itemId);
    formData.append("new-ordinal", newItemOrdinal);
    formData.append("new-priority", newItemPriority);

    fetch("/item-order", {
        body: formData,
        method: "post"
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
    foreach_elem(".todo-item .drag-handle", function(el) {
        el.setAttribute('draggable', true);

        el.ondragstart = function(ev) {
            var itemId = el.getAttribute('itemid');

            ev.dataTransfer.dropEffect = 'move';
            ev.dataTransfer.setData('text/plain', itemId);

            var rowElem = elemById("item_row_" + itemId);

            ev.dataTransfer.setDragImage(rowElem, 0, 0);
        };
    });

    foreach_elem('.todo-item.order-drop-target', function(el) {
        makeDropTarget(el, function(ev) {
            ev.preventDefault();

            var itemId = ev.dataTransfer.getData("text/plain");
            var newItemOrdinal = ev.currentTarget.getAttribute('ordinal');
            var newItemPriority = ev.currentTarget.getAttribute('priority');

            doSetItemOrder(itemId, newItemOrdinal, newItemPriority);
        });
    });

    foreach_elem('.list-list .list-row', function(el) {
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

function onNewItemInputKeydown(event) {
    if (event.ctrlKey && event.keyCode == 13) {
        event.preventDefault();
        event.stopPropagation();

        submitHighPriority();
    }
}

function initTodoList() {
    setupEditableItems();
    setupItemDragging();
};

function initNewUser() {
    elemById("password1").onkeyup = checkPasswords;
    elemById("password1").onchange = checkPasswords;
    elemById("password2").onkeyup = checkPasswords;
    elemById("password2").onchange = checkPasswords;
};

const initFns = {
    'todo-list': initTodoList,
    'init-new-user': initNewUser
};

document.addEventListener("turbolinks:load", function() {
    let body = document.getElementsByTagName('body')[0];

    let initFn = initFns[body.getAttribute('data-class')];

    if (initFn) {
        initFn();
    }
});

Turbolinks.savedScrolls = {};

function saveScrolls() {
    let nodes = document.querySelectorAll("[data-preserve-scroll=true]");

    foreach_elem("[data-preserve-scroll=true]", function(elem) {
        Turbolinks.savedScrolls[elem.id] = elem.scrollTop;
    });
}

document.addEventListener("turbolinks:before-visit", function(event) {
    saveScrolls();
});

document.addEventListener("turbolinks:render", function(event) {
    setupSidebar();

    const autofocusedElements = document.querySelectorAll('input[autofocus]');

    if (autofocusedElements.length) {
        autofocusedElements[0].focus();
    }

    for(const id in Turbolinks.savedScrolls) {
        const elem = elemOptionalById(id);

        if (elem) {
            elem.scrollTop = Turbolinks.savedScrolls[elem.id];
        }
    }
});

// startup

document.addEventListener('DOMContentLoaded', function() {
    Turbolinks.start();
    setupSidebar();
}, false);


