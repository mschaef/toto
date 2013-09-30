/* toto-todo-list.js */

$(document).ready(function () {
    $("#item-description").focus();

    $( ".item-list .item-row" ).draggable({
      appendTo: "body",
      helper: "clone"
      });

    $( ".list-list li" ).droppable({
      hoverClass: "drop-hover",
      accept: ":not(.ui-sortable-helper)",
      drop: function( event, ui ) {
        var itemId = $(ui.draggable[0]).attr("itemid");
        var newListId = $(this).attr("listid");

        $("#item_set_list_" + itemId + " #target-list")[0].value = newListId;
        $("#item_set_list_" + itemId)[0].submit();
      }
    });
});
