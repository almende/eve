/*
 expandable menu
 http://jasalguero.com/ledld/development/web/expandable-list/
 */

var loaded = false;

function prepareMenu() {
    $('#menu-tree').find('li:has(ul)')
        .click( function(event) {
            if (this == event.target) {
                $(this).toggleClass('expanded');
                $(this).children('ul').toggle(loaded ? 400: 0);

                storeExpanded();

                return false;
            }
        })
        .addClass('collapsed')
        .children('ul').hide();

    loadExpanded();

    loaded = true;
}

// store the expanded menu items in local storage
function storeExpanded () {
    var ids = [];
    $('#menu-tree').find('.expanded').each(function (index, elem) {
        if (elem.id) {
            ids.push(elem.id);
        }
    });

    if (window['localStorage']) {
        localStorage['expanded'] = JSON.stringify(ids);
    }

}

// load the expanded menu items
function loadExpanded() {
    try {
        if (window['localStorage']) {
            var expanded = ['concepts']; // default
            if (localStorage['expanded']) {
                var expanded = JSON.parse(localStorage['expanded']);
            }

            expanded.forEach(function (id) {
                $('#' + id).click();
            });
        }
    }
    catch(e) {}
}

$(document).ready( function() {
    prepareMenu()
});
