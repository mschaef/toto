/* toto.js */

sidebarVisible = false;


function toggleSidebar(e)
{
    e.preventDefault();
 
    var $body = $( 'body' ),
        $contents = $( '#contents' ),
        $menu = $( '#sidebar' ),
 
        /* Cross browser support for CSS "transition end" event */
        transitionEnd = 'transitionend webkitTransitionEnd otransitionend MSTransitionEnd';
 
    /* When the toggle menu link is clicked, animation starts */
    $body.addClass( 'animating' );
 
    /***
     * Determine the direction of the animation and
     * add the correct direction class depending
     * on whether the menu was already visible.
     */
    if ( $body.hasClass( 'menu-visible' ) ) {
        $body.addClass( 'left' );
    } else {
        $body.addClass( 'right' );
    }
  
    /***
     * When the animation (technically a CSS transition)
     * has finished, remove all animating classes and
     * either add or remove the "menu-visible" class 
     * depending whether it was visible or not previously.
     */
    $contents.on( transitionEnd, function() {
        $body
            .removeClass( 'animating left right' )
            .toggleClass( 'menu-visible' );
 
        $contents.off( transitionEnd );
    } );
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
