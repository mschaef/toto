/* toto.js */

sidebarVisible = false;


function toggleSidebar()
{
    $('#overlay').animate({left: (sidebarVisible ? "-270px" : "0px") });
    $('#header').animate({left: (sidebarVisible ? "0px" : "270px") });

    $('.wrapper').animate({"left": (sidebarVisible ? "0px" : "270px") });


    sidebarVisible = !sidebarVisible;
}

function refreshPage()
{
    location.reload();
}

function setItemPriority(itemId, newPriority) {
    $.post("/item/" + itemId + "/priority",
           { "new-priority": newPriority },
           function(data) {
               refreshPage();
           });
}

function completeItem(itemId) {
    $.post("/item/" + itemId + "/complete",
           function(data) {
               refreshPage();
           });
}

function restoreItem(itemId) {
    $.post("/item/" + itemId + "/restore",
           function(data) {
               refreshPage();
           });
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

    formMarkup += "<td colspan=\"2\"><form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width simple-border\" id=\"list-description\" name=\"list-description\" type=\"text\" maxlength=\"32\"/>";
    formMarkup += "</form></td>";

    $('td.add-list').replaceWith(formMarkup);

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
  formMarkup += "<a href=\"javascript:deleteItem(" + itemId + ")\"><i class=\"fa fa-trash-o icon-black\"></i></a>";
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
