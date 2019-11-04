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

function elemById(id) {
    var element = document.getElementById(id);

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
        if(onSingleTap) {
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

function toggleSidebar() {
    var menu = elem('sidebar');

    sidebarVisible = !sidebarVisible;

    menu.classList.toggle('menu-visible', sidebarVisible);
}

function onToggleSidebar(e) {
    e.preventDefault();

    toggleSidebar();
}

function refreshPage()
{
    location.reload();
}

function deleteItem(itemId) {
    fetch("/item/" + itemId + "/delete", {
        method: 'POST'
    }).then(function(data) {
        refreshPage();
    });
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
  var formMarkup = "";

  var itemDesc = elemById('item_desc_' + itemId).textContent;

  formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
  formMarkup += "<button type=\"submit\" class=\"item-button\"><i class=\"fa fa-trash-o icon-black\"></i></button>";
  formMarkup += "</form>";

  elemById('item_control_' + itemId).outerHTML = formMarkup;

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
        closeMenu.ontouchstart = onToggleSidebar;
        closeMenu.onclick = onToggleSidebar;
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

var pageInit = {};

//////// todo list

function setupEditableItems() {
    foreach_elem('.item-list .item-row .item-description', function(el) {
        el.ontouchstart = doubleTapFilter(null, function(obj) {
            beginItemEdit(el.getAttribute('itemid'));
        });
        el.onclick = doubleTapFilter(null, function(obj) {
            beginItemEdit(el.getAttribute('itemid'));
        });
    });
}

function doMoveItem(itemId, newListId) {
    elemById("target-item").value = itemId;
    elemById("target-list").value = newListId;
    elemById("item_set_list_form").submit();
}

function setupItemDragging() {
    foreach_elem(".item-list .item-row", function(el) {
        el.setAttribute('draggable', true);

        el.ondragstart = function(ev) {
            ev.dataTransfer.dropEffect = 'move';
            ev.dataTransfer.setData('text/plain', el.getAttribute('itemid'));
        };
    });

    foreach_elem('.list-list tr.list-row', function(el) {
        var dragCount = 0;

        el.ondragenter = function(ev) {
            ev.preventDefault();

            if (!dragCount)
                el.classList.add('drop-hover');

            dragCount++;
        };

        el.ondragleave = function(ev) {
            ev.preventDefault();

            dragCount--;

            if (!dragCount)
                el.classList.remove('drop-hover');
        };

        el.ondragover = function(ev) {
            ev.preventDefault();
        };

        el.ondrop = function(ev) {
            ev.preventDefault();

            var itemId = ev.dataTransfer.getData("text/plain");
            var newListId = ev.currentTarget.getAttribute('listid');

            doMoveItem(itemId, newListId);
        };
    });
}

function setupNewItemForm() {
    var starToggle = elemBySelector('.new-item-form i');
    var priorityField = elemBySelector('.new-item-form #item-priority');

    starToggle.onclick = function() {
        if (starToggle.className === "fa fa-lg fa-star-o icon-gray") {
            starToggle.className = "fa fa-lg fa-star icon-yellow";
            priorityField.value = "1";
        } else {
            starToggle.className = "fa fa-lg fa-star-o icon-gray";
            priorityField.value = "0";
        }
    };
}

pageInit["todo-list"] = function () {
    setupEditableItems();
    setupItemDragging();
    setupNewItemForm();
};

pageInit["new-user"] = function () {
    elemById("password1").onkeyup = checkPasswords;
    elemById("password1").onchange = checkPasswords;
    elemById("password2").onkeyup = checkPasswords;
    elemById("password2").onchange = checkPasswords;
};

function pageInitializer(initMap) {
    return function() {
        var pageInitFn = pageInit[initMap.page];

        if (pageInitFn != null) {
            pageInitFn();
        }

        setupSidebar();
    };
}

function totoInitialize(initMap) {
    document.addEventListener('DOMContentLoaded', pageInitializer(initMap));
}
