/* toto.js */

function beginListCreate()
{
    var formMarkup = ""

    formMarkup += "<form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width\" id=\"list-description\" name=\"list-description\" type=\"text\" />";
    formMarkup += "</form>";

    $('p.new-list').replaceWith(formMarkup);
}

function beginItemEdit(itemId, itemDesc)
{
    var formMarkup = ""

    formMarkup += "<form action=\"/item/" + itemId + "\" method=\"POST\">";
    formMarkup += "<input class=\"full-width\" id=\"description\" name=\"description\" type=\"text\" value=\"" + itemDesc + "\"/>";
    formMarkup += "</form>";

    $('div#item_' + itemId).replaceWith(formMarkup);
}