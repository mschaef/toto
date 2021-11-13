
/* toto.js */

function elemOptional(klass) {
    var elements = document.getElementsByClassName(klass);

    if (elements.length > 0) {
        return elements[0];
    }

    return null;
}

function elem(klass) {
    var element =  elemOptional(klass);

    if(!element) {
        console.error('Expected missing element with class: ' + klass);
    }

    return element;
}

function elemOptionalById(id) {
    return document.getElementById(id);
}

function elemById(id) {
    var element = elemOptionalById(id);

    if(!element) {
        console.error('Expected missing element with id: ' + id);
    }

    return element;
}

function elemBySelector(selector) {
    var elements = document.querySelectorAll(selector);

    if (elements.length == 0) {
        console.error('Expected missing element with selector: ' + selector);
    } else if (elements.length > 0) {
        if (elements.length > 1) {
            console.error('Warning: more than one element with selector: ' + selector);
        }

v        return elements[0];
    }

    return null;
}

function foreach_elem(selector, fn) {
    Array.prototype.forEach.call(document.querySelectorAll(selector), function(el, i) { fn(el); });
}

function doubleTapFilter(onSingleTap, onDoubleTap) {
    var DOUBLETAP_TIME_MSEC = 300;

    var doubletapTimer_ = null;

    function doubletapTimeout() {
        if (onSingleTap) {
            onSingleTap();
        }

        doubletapTimer_ = null;
    }

    return function() {
        if (doubletapTimer_) {
            clearTimeout(doubletapTimer_);
            doubletapTimer_ = null;
            if(onDoubleTap) {
                onDoubleTap();
            }
        } else {
            doubletapTimer_ = setTimeout(doubletapTimeout, DOUBLETAP_TIME_MSEC);
        }
    };
}

sidebarVisible = false;

function onToggleSidebar(evt) {
    evt.preventDefault();

    var menu = elem('sidebar');

    if (menu.contains(evt.target)) {
        return;
    }

    doToggleSidebar(evt);
}

function doToggleSidebar(evt) {
    evt.preventDefault();

    var menu = elem('sidebar');

    sidebarVisible = !sidebarVisible;

    menu.classList.toggle('menu-visible', sidebarVisible);

    if(sidebarVisible) {
        document.addEventListener("mousedown", onToggleSidebar);
    } else {
        document.removeEventListener("mousedown", onToggleSidebar);
    }
}

function visitPage(target)
{
    saveScrolls();
    Turbolinks.clearCache();
    Turbolinks.visit(target);
}

function refreshPage()
{
    visitPage(location);
}

function setupSidebar() {
    var toggleMenu = elemOptional('toggle-menu');

    if (toggleMenu) {
        toggleMenu.ontouchstart =  onToggleSidebar;
        toggleMenu.onclick =  onToggleSidebar;
    }

    var closeMenu = elemOptional('close-menu');

    if (closeMenu) {
        closeMenu.ontouchstart = doToggleSidebar;
        closeMenu.onclick = doToggleSidebar;
    }
};

function checkPasswords()
{
    var pwd1 = elemById("password1").value.trim();
    var pwd2 = elemById("password2").value.trim();

    var errDiv = elemById("error");

    if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
        errDiv.innerHTML = "Passwords do not match";
    else
        errDiv.innerHTML = "";
}

function doPost(baseUrl, args, nextUrl) {
    var queryArgs = [];

    for(const argName in args) {
        queryArgs.push((argName + '=' + args[argName]));
    }

    var url = baseUrl;

    if (queryArgs.length) {
        url += ('?' + queryArgs.join('&'));
    }

    function doRefresh() {
        if (nextUrl) {
            visitPage(nextUrl);
        } else {
            refreshPage();
        }
    }

    fetch(url, {
        method: 'POST',
        credentials: 'include'
    }).then(doRefresh);
}

function doMoveItem(itemId, newListId) {
    var formData = new FormData();

    formData.append("target-item", itemId);
    formData.append("target-list", newListId);

    fetch("/item-list", {
        body: formData,
        method: "POST",
        credentials: 'include'
    }).then(refreshPage);
}

function makeDropTarget(el, onDrop) {
    var dragCount = 0;

    function updateTarget() {
        if (dragCount > 0) {
            el.classList.add('drop-hover');
        } else {
            el.classList.remove('drop-hover');
        }
    }

    el.ondragenter = function(ev) {
        ev.preventDefault();

        dragCount++;
        updateTarget();
    };

    el.ondragleave = function(ev) {
        ev.preventDefault();

        dragCount--;
        updateTarget();
    };

    el.ondragover = function(ev) {
        ev.preventDefault();
    };

    el.ondrop = function(ev) {
        ev.preventDefault();

        dragCount = 0;
        updateTarget();

        onDrop(ev);
    };

    el.ondragend = function(ev) {
        ev.preventDefault();

        dragCount = 0;
        updateTarget();
    };
}

function dismissModalIfPresent() {
    var modal = elemOptional('modal');

    var nextUrl;

    if (modal) {
        nextUrl = modal.getAttribute('data-escape-url');
    }

    visitPage(nextUrl);
}

function checkModalShortcutBindings(event) {
    var modal = elemOptional('modal');

    if (!modal) {
        return;
    }

    var shortcuts = modal.querySelectorAll('span[data-shortcut-key]');

    for(let shortcut of shortcuts) {
        if (event.key === shortcut.getAttribute('data-shortcut-key')) {
            event.preventDefault();
            shortcut.onclick();
        }
    }
}

function onDocumentKeydown(event) {
    if (event.keyCode == 27) {
        dismissModalIfPresent();
    } else {
        checkModalShortcutBindings(event);
    }
}

function onDocumentClick(event) {
    var modalBackground = elemOptional('modal-background');

    if (modalBackground && (event.target == modalBackground)) {
        dismissModalIfPresent();
    }
}


function initNewUser() {
    elemById("password1").onkeyup = checkPasswords;
    elemById("password1").onchange = checkPasswords;
    elemById("password2").onkeyup = checkPasswords;
    elemById("password2").onchange = checkPasswords;
};

const initFns = {
    'init-new-user': initNewUser
};

document.addEventListener("turbolinks:load", function() {
    let body = document.getElementsByTagName('body')[0];

    let initFn = initFns[body.getAttribute('data-class')];

    if (initFn) {
        initFn();
    }
});

Turbolinks.savedScrolls = {};

function saveScrolls() {
    let nodes = document.querySelectorAll("[data-preserve-scroll=true]");

    foreach_elem("[data-preserve-scroll=true]", function(elem) {
        Turbolinks.savedScrolls[elem.id] = elem.scrollTop;
    });
}

document.addEventListener("turbolinks:before-visit", function(event) {
    saveScrolls();
});

document.addEventListener("turbolinks:render", function(event) {
    setupSidebar();

    const autofocusedElements = document.querySelectorAll('input[autofocus]');

    if (autofocusedElements.length) {
        autofocusedElements[0].focus();
    }

    for(const id in Turbolinks.savedScrolls) {
        const elem = elemOptionalById(id);

        if (elem) {
            elem.scrollTop = Turbolinks.savedScrolls[elem.id];
        }
    }
});

// startup

document.addEventListener('DOMContentLoaded', function() {
    Turbolinks.start();
    setupSidebar();
}, false);

document.addEventListener("keydown", onDocumentKeydown);
document.addEventListener("click", onDocumentClick);
