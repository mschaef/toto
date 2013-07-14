/* toto.js */

function beginListCreate()
{
    var formMarkup = ""

    formMarkup += "<form action=\"/list\" method=\"POST\">";
    formMarkup += "<input class=\"full-width\" id=\"list-description\" name=\"list-description\" type=\"text\" />";
    formMarkup += "</form>";

    $('p.new-list').replaceWith(formMarkup);
}