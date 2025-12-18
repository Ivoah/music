<!DOCTYPE html>
<html>
    <head>
        <title>Noah is listening to {{song['title'] if song is not None else 'nothing'}}</title>  

        <link rel="stylesheet" href="/static/style.css" />
        <link rel="shortcut icon" type="image/png" href="/static/favicon.png" />

        <script src="https://d3js.org/d3.v5.min.js"></script>
        <script src="https://code.jquery.com/jquery-3.6.0.min.js" crossorigin="anonymous"></script>
        <script src="/static/music.js"></script>

        <!-- <meta http-equiv="refresh" content="60" /> -->
    </head>
    <body>
        %if song is not None:
            <img class="center" style="filter: blur(100px);" src="{{song['artwork']}}">
            <img class="center hidden artwork" src="{{song['artwork']}}">
        %end
        <div class="center textbox">
            %if song is not None:
                Noah is listening to <b><a href="{{song['link']}}" onmouseenter="showArtwork()" onmouseleave="hideArtwork()">{{song['title']}}</a></b> right now
            %else:
                Noah is listening to nothing right now
            %end
        </div>
        <div class="bottom">
            <div class="flexbox">
                <svg id="labels"></svg>
                <div class="scrollbox"><svg id="chart"></svg></div>
            </div>
        </div>
    </body>
</html>
