/* toto.js */

function beginListCreate()
{
    var formMarkup = ""

    formMarkup += "<form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width\" id=\"list-description\" name=\"list-description\" type=\"text\" />";
    formMarkup += "</form>";

    $('p.new-list').replaceWith(formMarkup);
}

function beginUserAdd(listId)
{
    var formMarkup = ""

    formMarkup += "<input class=\"full-width\" id=\"share-with-email\" name=\"share-with-email\" type=\"text\" />";

    $('p.new-user').replaceWith(formMarkup);
}

function beginItemEdit(itemId, itemDesc)
{
    var formMarkup = ""

    formMarkup += "<form class=\"embedded\" action=\"/item/" + itemId + "/delete\" method=\"POST\">";
    formMarkup += "<input alt=\"Delete Item\" height=\"11\" src=\"/x_11x11.png\" type=\"image\" width=\"11\" />";
    formMarkup += "</form>";

    formMarkup += "<form  class=\"embedded\" action=\"/item/" + itemId + "\" method=\"POST\">";
    formMarkup += "<input class=\"full-width\" id=\"description\" name=\"description\" type=\"text\" value=\"" + itemDesc + "\"/>";
    formMarkup += "</form>";

    $('div#item_' + itemId).replaceWith(formMarkup);
}

function beginListEdit(listId, listDesc)
{
    var formMarkup = ""

    formMarkup += "<form class=\"embedded\" action=\"/list/" + listId + "/delete\" method=\"POST\">";
    formMarkup += "<input alt=\"Delete List\" height=\"11\" src=\"/x_11x11.png\" type=\"image\" width=\"11\" />";
    formMarkup += "</form>";

    formMarkup += "<form class=\"embedded\"  action=\"/list/" + listId + "/description\" method=\"POST\">";
    formMarkup += "<input id=\"description\" name=\"description\" type=\"text\" value=\"" + listDesc + "\"/>";
    formMarkup += "</form>";

    $('span#list_' + listId).replaceWith(formMarkup);
}


