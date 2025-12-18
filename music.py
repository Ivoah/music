import os
import pymysql
import json
import pymysql.cursors
from bottle import *
from datetime import datetime
import collections

try:
    os.chdir(os.path.dirname(__file__))
except FileNotFoundError:
    pass

REDIRECT = False

def db_connection():
    return pymysql.connect(host='***', user='**', password='***', db='***', cursorclass=pymysql.cursors.DictCursor)

@get('/')
def main():
    with db_connection() as db, db.cursor() as cursor:
        cursor.execute('''
            SELECT *
            FROM (
                SELECT
                JSON_VALUE(data, '$.item.name') 'title',
                JSON_VALUE(data, '$.item.external_urls.spotify') 'link',
                JSON_VALUE(data, '$.item.album.images[0].url') 'artwork',
                TIMESTAMPDIFF(second, time, NOW()) < 90 AND JSON_VALUE(data, '$.is_playing') 'active'
                FROM music
                ORDER BY time DESC
                LIMIT 1
            ) t
            WHERE active
        ''')
        song = cursor.fetchone()

    return template('music.tpl', song=song)

@get('/history.json')
def history():
    response.headers.append('Access-Control-Allow-Origin', '*')
    response.content_type = 'application/json'

    yield '[\n'
    with db_connection() as db, db.cursor() as cursor:
        cursor.execute('''
            SELECT
                time,
                JSON_VALUE(data, '$.item.name') 'song',
                CAST(JSON_VALUE(data, '$.progress_ms') AS INTEGER) 'progress'
            FROM music
            WHERE JSON_VALUE(data, '$.is_playing')
            ORDER BY time ASC
        ''')
        # cursor.execute('''
        #     SELECT time, song, progress
        #     FROM music.history
        #     ORDER BY time ASC
        # ''')
        songs = []
        for row in cursor.fetchall():
            if not songs:
                songs = [row]
            if (row['time'] - songs[-1]['time']) > timedelta(minutes=5.5):
                date = datetime.combine(songs[0]['time'].date(), datetime.min.time())
                yield json.dumps({
                    'date': date.date().isoformat(),
                    'span': [(songs[0]['time'] - date).total_seconds()//60, (songs[-1]['time'] - date).total_seconds()//60],
                    'songs': [song['song'] for song in songs]
                }) + ',\n'
                songs = [row]
            if (row['song'] == songs[-1]['song'] and row['progress'] >= songs[-1]['progress']):
                songs.pop()
            songs.append(row)

        date = datetime.combine(songs[0]['time'].date(), datetime.min.time())
        yield json.dumps({
            'date': date.date().isoformat(),
            'span': [(songs[0]['time'] - date).total_seconds()//60, (songs[-1]['time'] - date).total_seconds()//60],
            'songs': [song['song'] for song in songs]
        }) + '\n]'
        
@get('/stats.json')
def stats():
    response.headers.append('Access-Control-Allow-Origin', '*')
    response.content_type = 'application/json'

    stats = {}
    with db_connection() as db, db.cursor() as cursor:
        cursor.execute('SET @jan1 = MAKEDATE(year(now()),1)')
        cursor.execute('''
            SELECT (SELECT count(*) FROM music_playing WHERE time >= @jan1 AND time < CURDATE())/DATEDIFF(CURDATE(), @jan1)*365 AS minutes
        ''')
        stats['extrapolated_minutes'] = float(cursor.fetchone()['minutes'])
    
    stats['song_counts'] = collections.OrderedDict(collections.Counter(sum((h['songs'] for h in json.loads(''.join(history()))), start=[])).most_common())

    return json.dumps(stats, indent=4, sort_keys=False)


application = default_app()

if __name__ == '__main__':
    get('/static/<filename>')(lambda filename: static_file(filename, root='static/'))
    run(app=application, host='0.0.0.0', port=8080, debug=True, reloader=True)
