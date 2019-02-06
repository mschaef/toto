/* toto.js */

function elem(id) {
    var element =  document.getElementById(id);

    if(!element) {
        console.error('Expected missing element with ID: ' + id);
    }
    
    return element;
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

  var itemDesc = elem('div#item_desc_' + itemId).textContent;

  formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
  formMarkup += "<button type=\"submit\" class=\"item-button\"><i class=\"fa fa-trash-o icon-black\"></i></button>";
  formMarkup += "</form>";

  elem('div#item_control_' + itemId).outerHTML = formMarkup;

  formMarkup = "";

  formMarkup += "<form id=\"iedit_" + itemId + "\"  class=\"embedded\" action=\"/item/" + itemId + "\" method=\"POST\">";
  formMarkup += "<input class=\"full-width simple-border\" id=\"description\" name=\"description\" type=\"text\"/>";
  formMarkup += "</form>";

  elem('div#item_' + itemId).outerHTML = formMarkup;

  elem("#iedit_" + itemId + " #description").textContent = itemDesc;
  elem("#iedit_" + itemId + " #description").focus();
}

$(document).ready(function () {
    $( '#toggle-menu' ).on('touchstart click', onToggleSidebar);
    $( '#close-menu' ).on('touchstart click', onToggleSidebar);
});

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

pageInit["todo-list"] = function () {
  elem("item-description").focus();

  $( ".item-list .item-row" ).dblclick(function (obj){
      beginItemEdit($(obj.delegateTarget).attr("itemid"));
  });

  if ($(".item-list .item-row").draggable == undefined)
      return;

  $( ".item-list .item-row" ).draggable({
      appendTo: "body",
      helper: function() {
          return $("<div class=\"drag-item\">Moving List Item</div>");
      },
      cursor: "hand",
      cursorAt: {
          left: 0,
          top: 0
      }
  });

  $( ".list-list tr" ).droppable({
    hoverClass: "drop-hover",
    accept: ":not(.ui-sortable-helper)",
    tolerance: "pointer",
    drop: function( event, ui ) {
      var itemId = $(ui.draggable[0]).attr("itemid");
      var newListId = $(this).attr("listid");

      $("#item_set_list_form #target-item")[0].value = itemId;
      $("#item_set_list_form #target-list")[0].value = newListId;
      $("#item_set_list_form")[0].submit();
      }
  });
};

pageInit["new-user"] = function () {
    elem("password1").onkeyup = checkPasswords;
    elem("password1").onchange = checkPasswords;
    elem("password2").onkeyup = checkPasswords;
    elem("password2").onchange = checkPasswords;
};

function totoInitialize(initMap) {
    var pageInitFn = pageInit[initMap.page];

    if (pageInitFn != null) {
        $(document).ready(pageInitFn);
    }
}
