/*
 expandable menu
 http://jasalguero.com/ledld/development/web/expandable-list/
 */

var loaded = false;

function prepareMenu() {
    var menuTree = $('#menu-tree');

    // make all expandable list items clickable
    menuTree.find('li:has(ul)')
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

    // restore the last state: expanded.collapsed items
    loadExpanded();

    // find the page where we are now
    menuTree.find('a').each(function (index, elem) {
        if (elem.href == location.href) {
            // highlight it
            var parent =  $(elem).parent();
            parent.addClass('current');

            // expand all elements to this path
            while (parent[0]) {
                if (parent.hasClass('collapsed') && !parent.hasClass('expanded')) {
                    parent.click();
                }

                parent = parent.parent();
            }
        }
    });

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
