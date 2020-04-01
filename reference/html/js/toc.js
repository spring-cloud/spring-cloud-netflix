var toctitle = document.getElementById('toctitle');
var path = window.location.pathname;
if (toctitle != null) {
    var oldtoc = toctitle.nextElementSibling;
    var newtoc = document.createElement('div');
    newtoc.setAttribute('id', 'tocbot');
    newtoc.setAttribute('class', 'js-toc desktop-toc');
    oldtoc.setAttribute('class', 'mobile-toc');
    oldtoc.parentNode.appendChild(newtoc);
    tocbot.init({
        contentSelector: '#content',
        headingSelector: 'h1, h2, h3, h4, h5',
        positionFixedSelector: 'body',
        fixedSidebarOffset: 90,
        smoothScroll: false
    });
    if (!path.endsWith("index.html") && !path.endsWith("/")) {
        var link = document.createElement("a");
        if (document.getElementById('index-link')) {
          indexLinkElement = document.querySelector('#index-link > p > a');
          linkHref = indexLinkElement.getAttribute("href");
          link.setAttribute("href", linkHref);
        } else {
          link.setAttribute("href", "index.html");
        }
        link.innerHTML = "<span><i class=\"fa fa-chevron-left\" aria-hidden=\"true\"></i></span> Back to index";
        var block = document.createElement("div");
        block.setAttribute('class', 'back-action');
        block.appendChild(link);
        var toc = document.getElementById('toc');
        var next = document.getElementById('toctitle').nextElementSibling;
        toc.insertBefore(block, next);
    }
}

var headerHtml = '<div id="header-spring">\n' +
    '<h1>\n' +
    '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0" y="0"\n' +
    'viewBox="0 0 245.8 45.3" style="enable-background:new 0 0 245.8 45.3;" xml:space="preserve">\n' +
    '<g id="logos">\n' +
    '<g>\n' +
    '<path class="st0" d="M39.4,3.7c-0.6,1.5-1.4,2.8-2.3,4c-3.9-4-9.3-6.4-15.2-6.4c-11.7,0-21.3,9.5-21.3,21.3\n' +
    'c0,6.2,2.6,11.7,6.8,15.6l0.8,0.7c3.7,3.1,8.5,5,13.7,5c11.2,0,20.4-8.7,21.2-19.8C43.7,18.7,42.1,11.8,39.4,3.7z M10.5,38.3\n' +
    'c-0.6,0.8-1.8,0.9-2.6,0.3C7.1,37.9,7,36.8,7.6,36c0.6-0.8,1.8-0.9,2.6-0.3C11,36.4,11.1,37.5,10.5,38.3z M39.3,31.9\n' +
    'c-5.2,7-16.5,4.6-23.6,5c0,0-1.3,0.1-2.6,0.3c0,0,0.5-0.2,1.1-0.4c5-1.7,7.4-2.1,10.5-3.7c5.8-3,11.5-9.4,12.7-16.1\n' +
    'c-2.2,6.4-8.9,12-14.9,14.2c-4.2,1.5-11.7,3-11.7,3c0,0-0.3-0.2-0.3-0.2c-5.1-2.5-5.3-13.6,4-17.1c4.1-1.6,8-0.7,12.4-1.8\n' +
    'C31.6,14.1,37,10.6,39.2,6C41.7,13.3,44.7,24.8,39.3,31.9z"/>\n' +
    '<g>\n' +
    '<path class="st0" d="M55.2,30.9c-0.5-0.3-0.9-0.9-0.9-1.6c0-1.1,0.8-1.9,1.9-1.9c0.4,0,0.7,0.1,1,0.3c2,1.3,4.1,2,5.9,2\n' +
    'c2,0,3.2-0.9,3.2-2.2v-0.1c0-1.6-2.2-2.2-4.6-2.9c-3-0.9-6.5-2.1-6.5-6.1v-0.1c0-3.9,3.2-6.3,7.4-6.3c2.2,0,4.5,0.6,6.5,1.7\n' +
    'c0.7,0.4,1.1,1,1.1,1.8c0,1.1-0.9,1.9-2,1.9c-0.4,0-0.6-0.1-0.9-0.2c-1.7-0.9-3.4-1.4-4.9-1.4c-1.8,0-2.9,0.9-2.9,2v0.1\n' +
    'c0,1.5,2.2,2.2,4.7,2.9c3,0.9,6.4,2.3,6.4,6v0.1c0,4.3-3.4,6.5-7.7,6.5C60.4,33.3,57.6,32.5,55.2,30.9z"/>\n' +
    '<path class="st0" d="M72.5,14.3c0-1.3,1-2.4,2.3-2.4c1.3,0,2.4,1.1,2.4,2.4v1.4c1.5-2.2,3.7-3.9,7-3.9c4.8,0,9.6,3.8,9.6,10.7\n' +
    'v0.1c0,6.8-4.7,10.7-9.6,10.7c-3.4,0-5.6-1.7-7-3.6V37c0,1.3-1.1,2.4-2.4,2.4c-1.3,0-2.3-1-2.3-2.4V14.3z M89.1,22.7L89.1,22.7\n' +
    'c0-4.1-2.7-6.7-5.9-6.7c-3.2,0-6,2.7-6,6.6v0.1c0,4,2.8,6.6,6,6.6C86.4,29.3,89.1,26.7,89.1,22.7z"/>\n' +
    '<path class="st0" d="M95.7,14.3c0-1.3,1-2.4,2.3-2.4c1.3,0,2.4,1.1,2.4,2.4v1.1c0.2-1.8,3.1-3.5,5.2-3.5c1.5,0,2.3,1,2.3,2.3\n' +
    'c0,1.3-0.8,2.1-1.9,2.3c-3.4,0.6-5.7,3.5-5.7,7.6V31c0,1.3-1.1,2.3-2.4,2.3c-1.3,0-2.3-1-2.3-2.3V14.3z"/>\n' +
    '<path class="st0" d="M109.7,14.3c0-1.3,1-2.4,2.3-2.4c1.3,0,2.4,1.1,2.4,2.4V31c0,1.3-1.1,2.3-2.4,2.3c-1.3,0-2.3-1-2.3-2.3V14.3\n' +
    'z"/>\n' +
    '<path class="st0" d="M116.9,14.3c0-1.3,1-2.4,2.3-2.4c1.3,0,2.4,1.1,2.4,2.4v1c1.3-1.9,3.2-3.4,6.5-3.4c4.7,0,7.4,3.1,7.4,7.9V31\n' +
    'c0,1.3-1,2.3-2.3,2.3c-1.3,0-2.4-1-2.4-2.3v-9.7c0-3.2-1.6-5-4.4-5c-2.7,0-4.7,1.9-4.7,5.1V31c0,1.3-1.1,2.3-2.4,2.3\n' +
    'c-1.3,0-2.3-1-2.3-2.3V14.3z"/>\n' +
    '<path class="st0" d="M156.2,11.9c-1.3,0-2.4,1.1-2.4,2.4v1.4c-1.5-2.2-3.7-3.9-7-3.9c-4.9,0-9.6,3.8-9.6,10.7v0.1\n' +
    'c0,6.8,4.7,10.7,9.6,10.7c3.4,0,5.6-1.7,7-3.6c-0.2,3.7-2.5,5.7-6.5,5.7c-2.4,0-4.5-0.6-6.3-1.6c-0.2-0.1-0.5-0.2-0.9-0.2\n' +
    'c-1.1,0-2,0.9-2,2c0,0.9,0.5,1.6,1.3,1.9c2.5,1.2,5.1,1.8,8,1.8c3.7,0,6.6-0.9,8.5-2.8c1.7-1.7,2.7-4.3,2.7-7.8V14.3\n' +
    'C158.5,13,157.5,11.9,156.2,11.9z M147.9,29.2c-3.2,0-5.9-2.5-5.9-6.6v-0.1c0-4,2.7-6.6,5.9-6.6c3.2,0,6,2.7,6,6.6v0.1\n' +
    'C153.9,26.6,151.1,29.2,147.9,29.2z"/>\n' +
    '<path class="st0" d="M114.5,6.3c0,1.3-1.1,2.4-2.4,2.4c-1.3,0-2.4-1.1-2.4-2.4c0-1.3,1.1-2.4,2.4-2.4\n' +
    'C113.4,3.9,114.5,4.9,114.5,6.3z"/>\n' +
    '</g>\n' +
    '</g>\n' +
    '</g>\n' +
    '</svg>\n' +
    '\n' +
    '</h1>\n' +
    '</div>';

var header = document.createElement("div");
header.innerHTML = headerHtml;
document.body.insertBefore(header, document.body.firstChild);
