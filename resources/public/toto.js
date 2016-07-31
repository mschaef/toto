/* toto.js */

sidebarVisible = false;

function toggleSidebar(e)
{
    e.preventDefault();
 
    var $menu = $( '#sidebar' );
 
    /* Cross browser support for CSS "transition end" event */
    transitionEnd = 'transitionend webkitTransitionEnd otransitionend MSTransitionEnd';

    sidebarVisible = !sidebarVisible;
    
    /* When the toggle menu link is clicked, animation starts */
    $menu.toggleClass('menu-visible', sidebarVisible);
}

function refreshPage()
{
    location.reload();
}

function deleteItem(itemId) {
    $.post("/item/" + itemId + "/delete",
           function(data) {
               refreshPage();
           });
}

function beginListCreate()
{
  var formMarkup = "";

    formMarkup += "<li><form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width simple-border\" id=\"list-description\" name=\"list-description\" type=\"text\" maxlength=\"32\"/>";
    formMarkup += "</form></li>";

    $('li.add-list').replaceWith(formMarkup);

    $("#list-description").focus();
}

function beginUserAdd(listId)
{
  var formMarkup = "";

    formMarkup += "<input class=\"full-width simple-border\" id=\"share-with-email\" name=\"share-with-email\" type=\"text\" />";

    $('p.new-user').replaceWith(formMarkup);

    $("#share-with-email").focus();
}

function beginItemEdit(itemId)
{
  var formMarkup = "";

  var itemDesc = $('div#item_desc_' + itemId).text();

  formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
  formMarkup += "<button type=\"submit\" class=\"item-button\"><i class=\"fa fa-trash-o icon-black\"></i></button>";
  formMarkup += "</form>";

  $('div#item_control_' + itemId).replaceWith(formMarkup);

  formMarkup = "";

  formMarkup += "<form id=\"iedit_" + itemId + "\"  class=\"embedded\" action=\"/item/" + itemId + "\" method=\"POST\">";
  formMarkup += "<input class=\"full-width simple-border\" id=\"description\" name=\"description\" type=\"text\"/>";
  formMarkup += "</form>";

  $('div#item_' + itemId).replaceWith(formMarkup);

  $("#iedit_" + itemId + " #description").val(itemDesc);
  $("#iedit_" + itemId + " #description").focus();
}


$(document).ready(function () {
    $( '#toggle-menu' ).on('touchstart click', toggleSidebar);
});

function checkPasswords()
{
    var pwd1 = $( "#password1" ).val().trim();
    var pwd2 = $( "#password2" ).val().trim();


    var errDiv = $("#error");
    
    if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
        errDiv.text("Passwords do not match");
    else
        errDiv.empty();
}

var pageInit = {};

//////// todo list

pageInit["todo-list"] = function () {
  $("#item-description").focus();

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

  $( ".list-list li" ).droppable({
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
    $( "#password1" ).keyup(checkPasswords);
    $( "#password1" ).change(checkPasswords);
    
    $( "#password2" ).keyup(checkPasswords);
    $( "#password2" ).change(checkPasswords);
};

function totoInitialize(initMap) {
    var pageInitFn = pageInit[initMap.page];

    if (pageInitFn != null) {
        $(document).ready(pageInitFn);
    }
}
