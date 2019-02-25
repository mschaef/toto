/* toto.js */

function elemOptional(id) {
    return document.getElementById(id);
}

function elem(id) {
    var element =  elemOptional(id);

    if(!element) {
        console.error('Expected missing element with ID: ' + id);
    }
    
    return element;
}

function foreach_elem(selector, fn) {
    Array.prototype.forEach.call(document.querySelectorAll(selector), function(el, i) { fn(el); });
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

function beginListCreate()
{
  var formMarkup = "";

  formMarkup += "<tr><td colspan=\"2\"><form action=\"/list\" method=\"POST\">";
  formMarkup += "<input class=\"full-width simple-border\" id=\"list-description\" name=\"list-description\" type=\"text\" maxlength=\"32\"/>";
  formMarkup += "</form></td></tr>";

  elem('add-list').outerHTML = formMarkup;
  elem('list-description').focus();
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

  var itemDesc = elem('item_desc_' + itemId).textContent;

  formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
  formMarkup += "<button type=\"submit\" class=\"item-button\"><i class=\"fa fa-trash-o icon-black\"></i></button>";
  formMarkup += "</form>";

  elem('item_control_' + itemId).outerHTML = formMarkup;

  formMarkup = "";

  formMarkup += "<form id=\"iedit_" + itemId + "\"  class=\"embedded\" action=\"/item/" + itemId + "\" method=\"POST\">";
  formMarkup += "<input class=\"full-width simple-border\" id=\"iedit_" + itemId + "_description\" name=\"description\" type=\"text\"/>";
  formMarkup += "</form>";

  elem('item_' + itemId).outerHTML = formMarkup;

  elem("iedit_" + itemId + "_description").value = itemDesc;
  elem("iedit_" + itemId + "_description").focus();
}

function setupSidebar() {
    elem('toggle-menu').ontouchstart =  onToggleSidebar;
    elem('toggle-menu').onclick =  onToggleSidebar;

    var closeMenu = elemOptional('close-menu');

    if (closeMenu) {
        closeMenu.ontouchstart = onToggleSidebar;
        closeMenu.onclick = onToggleSidebar;
    }
};

function checkPasswords()
{
    var pwd1 = elem("password1").value.trim();
    var pwd2 = elem("password2").value.trim();

    var errDiv = elem("error");
    
    if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
        errDiv.innerHTML = "Passwords do not match";
    else
        errDiv.innerHTML = "";
}

var pageInit = {};

//////// todo list

function setupEditableItems() {
    foreach_elem('.item-list .item-row', function(el) {
        el.ondblclick = function(obj) {
            beginItemEdit(el.getAttribute('itemid'));
        };
    });
}

function doMoveItem(itemId, newListId) {
    elem("target-item").value = itemId;
    elem("target-list").value = newListId;
    elem("item_set_list_form").submit();
}

function setupItemDragging() {
    foreach_elem(".item-list .item-row", function(el) {
        el.setAttribute('draggable', true);
        
        el.ondragstart = function(ev) {
            ev.dataTransfer.dropEffect = 'move';
            ev.dataTransfer.setData('text/plain', el.getAttribute('itemid'));
        };
    });

    foreach_elem('.list-list tr', function(el) {
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

pageInit["todo-list"] = function () {
    elem("item-description").focus();
    setupEditableItems();
    setupItemDragging();
};

pageInit["new-user"] = function () {
    elem("password1").onkeyup = checkPasswords;
    elem("password1").onchange = checkPasswords;
    elem("password2").onkeyup = checkPasswords;
    elem("password2").onchange = checkPasswords;
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
