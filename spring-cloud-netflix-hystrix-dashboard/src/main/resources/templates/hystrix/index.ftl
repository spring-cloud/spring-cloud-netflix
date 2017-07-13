<#import "/spring.ftl" as spring />
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Hystrix Dashboard</title>
        <style type="text/css">
            #main {
                width: 800px;
                margin: 0 auto;
                text-align: center;
            }
            #logo {
                width: 264px;
                height: 233px;
            }
            #message {
                color: red;
            }
        </style>
    </head>
    <body>
        <div id="main">
            <img id="logo" src="hystrix/images/hystrix-logo.png">
            <br><br>
            <h2>Hystrix Dashboard</h2>
            <input id="stream" type="textfield" size="120" placeholder="http://hostname:port/turbine/turbine.stream"></input>
            <br><br>
            <i>Cluster via Turbine (default cluster):</i> http://turbine-hostname:port/turbine.stream
            <br>
            <i>Cluster via Turbine (custom cluster):</i> http://turbine-hostname:port/turbine.stream?cluster=[clusterName]
            <br>
            <i>Single Hystrix App:</i> http://hystrix-app:port/hystrix.stream
            <br><br>
            Delay: <input id="delay" type="textfield" size="10" placeholder="2000"></input>ms 
            &nbsp;&nbsp;&nbsp;&nbsp; 
            Title: <input id="title" type="textfield" size="60" placeholder="Example Hystrix App"></input><br>
            <br>
            <button id="monitor">Monitor Stream</button>
            <br><br>
            <div id="message"></div>
        </div>
        
        <!-- Javascript to monitor and display -->
        <script src="webjars/jquery/2.1.1/jquery.min.js" type="text/javascript"></script>
        <script type="text/javascript">
            'use strict';
            $(window).onload(function () {
                $('#monitor').on('click', function () {
                    const stream = $('#stream').val().trim();
                    const delay = $('#delay').val().trim();
                    const title = $('#title').val().trim();

                    if (stream.length == 0) {
                        $('#message').html("The 'stream' value is required.");
                        return;
                    }

                    let url = "hystrix/monitor?stream=" + encodeURIComponent(stream);
                    if (delay.length > 0) {	
                        url += "&delay=" + delay;
                    }
                    if (title.length > 0) {	
                        url += "&title=" + encodeURIComponent(title);
                    }
                    location.href = url;
                });
            });
        </script>
    </body>
</html>
