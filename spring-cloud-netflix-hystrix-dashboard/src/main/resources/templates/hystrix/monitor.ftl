<#import "/spring.ftl" as spring />
<!doctype html>
<html lang="en">
    <head>
        <meta charset="utf-8" />
        <title>Hystrix Monitor</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />

        <!-- Setup base for everything -->
        <link rel="stylesheet" type="text/css" href="css/global.css" />
        <!-- Our custom CSS -->
        <link rel="stylesheet" type="text/css" href="css/monitor.css" />
        <!-- HystrixCommand -->
        <link rel="stylesheet" type="text/css" href="components/hystrixCommand/hystrixCommand.css" />
        <!-- HystrixThreadPool -->
        <link rel="stylesheet" type="text/css" href="components/hystrixThreadPool/hystrixThreadPool.css" />
    </head>
    <body>
        <div id="header">
            <h2><span id="title_name"></span></h2>
        </div>

        <div class="container">
            <div class="row">
                <div class="menubar">
                    <div class="title">Circuit</div>
                    <div class="menu_actions">
                        Sort: 
                        <a id="circuit-errorThenVolume" href="#">Error then Volume</a> |
                        <a id="circuit-alphabetic" href="#">Alphabetical</a> | 
                        <a id="circuit-volume" href="#">Volume</a> | 
                        <a id="circuit-error" href="#">Error</a> | 
                        <a id="circuit-latencyMean" href="#">Mean</a> | 
                        <a id="circuit-latencyMedian" href="#">Median</a> | 
                        <a id="circuit-latency90" href="#">90</a> | 
                        <a id="circuit-latency99" href="#">99</a> | 
                        <a id="circuit-latency995" href="#">99.5</a> 
                    </div>
                    <div class="menu_legend">
                        <span class="success">Success</span> |
                        <span class="shortCircuited">Short-Circuited</span> |
                        <span class="badRequest">Bad Request</span> |
                        <span class="timeout">Timeout</span> |
                        <span class="rejected">Rejected</span> |
                        <span class="failure">Failure</span> |
                        <span class="errorPercentage">Error %</span>
                    </div>
                </div>
            </div>
            <div id="dependencies" class="row dependencies"><span class="loading">Loading&hellip;</span></div>

            <div class="spacer"></div>

            <div class="row">
                <div class="menubar">
                    <div class="title">Thread Pools</div>
                    <div class="menu_actions">
                        Sort:
                        <a id="pool-alphabetic" href="#">Alphabetical</a> | 
                        <a id="pool-volume" href="#">Volume</a>
                    </div>
                </div>
            </div>
            <div id="dependencyThreadPools" class="row dependencyThreadPools"><span class="loading">Loading&hellip;</span></div>
        </div>

        <!-- d3 -->
        <script type="text/javascript" src="webjars/d3js/3.4.11/d3.min.js" ></script>

        <!-- Javascript to monitor and display -->
        <script type="text/javascript" src="webjars/jquery/2.1.1/jquery.min.js" ></script>
        <script type="text/javascript" src="js/jquery.tinysort.min.js"></script>
        <script type="text/javascript" src="js/tmpl.js"></script>
        
        <script type="text/javascript" src="components/hystrixCommand/hystrixCommand.js"></script>
        <script type="text/javascript" src="components/hystrixThreadPool/hystrixThreadPool.js"></script>

        <script type="text/javascript">
        'use strict';
        /**
         * Queue up the monitor to start once the page has finished loading.
         * 
         * This is an inline script and expects to execute once on page load.
         */ 

        // commands
        const hystrixMonitor = new HystrixCommandMonitor('dependencies', {includeDetailIcon:false});
        // thread pool
        const dependencyThreadPoolMonitor = new HystrixThreadPoolMonitor('dependencyThreadPools');
        
        const urlVars = getUrlVars();
        let stream = urlVars["stream"];
        console.log("Stream: " + stream)

        if (stream != undefined) {
            if (urlVars["delay"] != undefined) {
                stream = stream + "&delay=" + urlVars["delay"];
            }
            const proxyUrl = "${contextPath}/proxy.stream?origin=" + stream;
            $('#title_name').text("Hystrix Stream: " + decodeURIComponent((urlVars["title"] == undefined) ? stream : urlVars["title"]));
        }
        
        let commandSource, poolSource;

        $(window).load(function() { // within load with a setTimeout to prevent the infinite spinner
            setTimeout(function() {
                if (proxyUrl == undefined) {
                    console.log("proxyUrl is undefined");
                    $("#dependencies .loading, #dependencyThreadPools .loading").html("The 'stream' argument was not provided.");
                    $("#dependencies .loading, #dependencyThreadPools .loading").addClass("failed");
                    return;
                }
                console.log("Proxy URL: " + proxyUrl);
                
                hystrixMonitor.sortByErrorThenVolume();
                commandSource = initStreamSource(proxyUrl, hystrixMonitor, '#dependencies');
                
                dependencyThreadPoolMonitor.sortByVolume();
                poolSource = initStreamSource(proxyUrl, dependencyThreadPoolMonitor, '#dependencyThreadPools');
            }, 0);
            
            setClickHandler('#circuit-errorThenVolume', hystrixMonitor.sortByErrorThenVolume);
            setClickHandler('#circuit-alphabetic', hystrixMonitor.sortAlphabetically);
            setClickHandler('#circuit-volume', hystrixMonitor.sortByVolume);
            setClickHandler('#circuit-error', hystrixMonitor.sortByError);
            setClickHandler('#circuit-latencyMean', hystrixMonitor.sortByLatencyMean);
            setClickHandler('#circuit-latencyMedian', hystrixMonitor.sortByLatencyMedian);
            setClickHandler('#circuit-latency90', hystrixMonitor.sortByLatency90);
            setClickHandler('#circuit-latency99', hystrixMonitor.sortByLatency99);
            setClickHandler('#circuit-latency995', hystrixMonitor.sortByLatency995);
            
            setClickHandler('#pool-alphabetic', dependencyThreadPoolMonitor.sortAlphabetically);
            setClickHandler('#pool-volume', dependencyThreadPoolMonitor.sortByVolume);
        });

        // Read a page's GET URL variables and return them as an associative array.
        // from: http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html
        function getUrlVars() {
            let vars = [], hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
            for (var i = 0; i < hashes.length; i++) {
                let hash = hashes[i].split('=');
                vars.push(hash[0]);
                vars[hash[0]] = hash[1];
            }
            return vars;
        }
        
        function initStreamSource(aStream, aMonitor, aParentId) {
            // start the EventSource which will open a streaming connection to the server
            const source = new EventSource(aStream);

            // add the listener that will process incoming events
            source.addEventListener('message', aMonitor.eventSourceMessageListener, false);

            //  source.addEventListener('open', function(e) {
            //      console.console.log(">>> opened connection, phase: " + e.eventPhase);
            //      // Connection was opened.
            //  }, false);
            
            source.addEventListener('error', function(e) {
                $(aParentId + " .loading").html("Unable to connect to Command Metric Stream.");
                $(aParentId + " .loading").addClass("failed");
                let errorMessage = (e.eventPhase == EventSource.CLOSED)
                    ? "Connection was closed on error: " // Connection was closed.
                    : "Error occurred while streaming: ";
                console.log(errorMessage + JSON.stringify(e));
            }, false);
            
            return source;
        }
        
        function setClickHandler(anId, aCallback) {
            $(anId).on('click', function (event) {
                event.preventDefault();
                aCallback();
            });
        }
        </script>
    </body>
</html>
