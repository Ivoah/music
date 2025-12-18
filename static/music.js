const formatDate = d3.utcFormat("%A %B %-d, %Y")
const formatTime = m => d3.timeFormat("%-I:%M %p")(new Date(2020, 0, 0, 0, m))
const formatChange = (y0, y1) => d3.format("+.2%")((y1 - y0) / y0)
const parseDate = d3.utcParse("%Y-%m-%d")

async function drawGraph() {
    const data = await d3.json("/history.json").then(data => data.map(row => { return {
        date: new Date(row.date),
        start: +row.span[0],
        stop: +row.span[1],
        songs: row.songs
    }}))

    const days = (data[data.length - 1].date - data[0].date)/(24*60*60*1000)
    const margin = {top: 20, right: 0, bottom: 30, left: 55}
    const width = Math.max(days*10 + 7, window.innerWidth - (margin.left + margin.right))
    const height = 250

    const x = d3.scaleBand()
        .domain(d3.utcDay.range(data[0].date, +data[data.length - 1].date + 1))
        .range([7, width - margin.right])
        .padding(0.2)

    const y = d3.scaleLinear()
        .domain([d3.min(data, d => d.start), d3.max(data, d => d.stop)])
        .rangeRound([height - margin.bottom, margin.top])

    const xAxis = g => g
        .attr("transform", `translate(0,${height - margin.bottom})`)
        .call(d3.axisBottom(x)
            .tickValues(d3.utcSunday
                .every(width > 720 ? 1 : 2)
                .range(data[0].date, data[data.length - 1].date))
            .tickFormat(d3.utcFormat("%-m/%-d")))
        .call(g => g.select(".domain").remove())

    const yAxis = g => g
        .call(d3.axisLeft(y)
            .tickFormat(formatTime)
            .tickValues(d3.scaleLinear().domain(y.domain()).ticks()))
        .call(g => {
            g.selectAll(".tick line")
            .attr("stroke-opacity", 0.2)
            .attr("x2", width - margin.right)
        })
        .call(g => g.select(".domain").remove())
        .call(g => g.selectAll(".tick text").remove())
    
    const yLabels = g => g
        .attr("transform", `translate(${margin.left},0)`)
        .call(d3.axisLeft(y)
            .tickFormat(formatTime)
            .tickValues(d3.scaleLinear().domain(y.domain()).ticks()))
        .call(g => g.select(".domain").remove())

    d3.selectAll("svg > *").remove()

    d3.select("svg#labels")
        .attr("width", margin.left)
        .attr("height", height)
        .append("g")
        .call(yLabels)

    const chart = d3.select("svg#chart")
        .attr("width", width)
        .attr("height", height)

    chart.append("g")
        .call(xAxis)

    chart.append("g")
        .call(yAxis)

    const g = chart.append("g")
        .attr("stroke-linecap", "round")
        .selectAll("line")
        .data(data)
        .join("line")
        .attr("x1", d => x(d.date))
        .attr("y1", d => y(d.start))
        .attr("x2", d => x(d.date))
        .attr("y2", d => y(d.stop))
        .attr("stroke", "lightblue")
        .attr("stroke-width", 7)
        .on("mouseenter", function(d, i) {
            d3.select(this)
            .transition()
            .duration(50)
            .attr("stroke-width", 10)
        })
        .on("mouseleave", function(d, i) {
            d3.select(this)
            .transition()
            .attr("stroke-width", 7)
        })
        .on("click", d => alert(hoverText(d)))

    g.append("title")
        .text(hoverText)

    $(".scrollbox").each(function(idx) {this.scrollLeft = this.scrollWidth})
}

function hoverText(d) {
    return `${formatDate(d.date)}
start: ${formatTime(d.start)}
stop: ${formatTime(d.stop)}
${d.stop - d.start} minutes total
songs:
${d.songs.map(s => `* ${s}`).join("\n")}`
}

function showArtwork(url) {
    $(".artwork").toggleClass("hidden", false)
}

function hideArtwork() {
    $(".artwork").toggleClass("hidden", true)
}

$(drawGraph)

timeout = null
window.addEventListener('resize', function() {
    // clear the timeout
    clearTimeout(timeout);
    // start timing for event "completion"
    timeout = setTimeout(drawGraph, 100);
});
