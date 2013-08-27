/*
 expandable menu
 http://jasalguero.com/ledld/development/web/expandable-list/
 */

function prepareMenu() {
    $('#menu-tree').find('li:has(ul)')
        .click( function(event) {
            if (this == event.target) {
                $(this).toggleClass('expanded');
                $(this).children('ul').toggle('medium');
                return false;
            }
        })
        .addClass('collapsed')
        .children('ul').hide();
}

$(document).ready( function() {
    prepareMenu()
});
