/*
 expandable menu
 http://jasalguero.com/ledld/development/web/expandable-list/
 */

function prepareMenu() {
    var menuTree = $('#menu-tree');

    // make all expandable list items clickable
    menuTree.find('.title')
        .click( function(event) {
            if (this == event.target) {
                var li = $(this).parent();
                toggleExpanded(li, 400);

                storeExpanded();

                return false;
            }
        })
        .parent()
        .addClass('collapsed')
        .children('ul').hide();

    // restore the last state: expanded.collapsed items
    loadExpanded();

    // find the page where we are now and highlight it
    menuTree.find('a').each(function (index, elem) {
        if (location.href.indexOf(elem.href) == 0) {
            var parent = $(elem).parent();
            parent.addClass('current');

            // expand all elements to this path
            while (parent[0]) {
                if (parent.hasClass('collapsed') && !parent.hasClass('expanded')) {
                    toggleExpanded(parent, 0);
                }

                parent = parent.parent();
            }
        }
    });
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
                expanded = JSON.parse(localStorage['expanded']);
            }

            expanded.forEach(function (id) {
                var li = $('#' + id);
                toggleExpanded(li, 0);
            });
        }
    }
    catch(e) {}
}

function toggleExpanded (elem, duration) {
    elem.toggleClass('expanded')
        .children('ul')
        .toggle(duration)
}

$(document).ready( function() {
    prepareMenu()
});
