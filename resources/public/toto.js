/* toto.js */

sidebarVisible = false;


function toggleSidebar()
{
    $('#overlay').animate({left: (sidebarVisible ? "-270px" : "0px") });
    $('#header').animate({left: (sidebarVisible ? "0px" : "270px") });

    $('#contents').animate({"margin-left": (sidebarVisible ? "0px" : "270px") });


    sidebarVisible = !sidebarVisible;
}

function beginListCreate()
{
  var formMarkup = "";

    formMarkup += "<form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width simple-border\" id=\"list-description\" name=\"list-description\" type=\"text\" maxlength=\"32\"/>";
    formMarkup += "</form>";

    $('p.new-list').replaceWith(formMarkup);

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
  formMarkup += "<input alt=\"Delete Item\" height=\"12\" src=\"/trash_stroke_12x12.png\" type=\"image\" width=\"12\" maxlength=\"1024\"/>";
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
