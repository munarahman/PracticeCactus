var express = require('express');
var app = express();
var http = require('http').Server(app);
var fs = require('fs');
var file = "practicedata.db";
var exists = fs.existsSync(file);
var bodyParser = require('body-parser');
var uuid = require('node-uuid');

var busboy = require('connect-busboy'); //middleware for form/file upload
var path = require('path');     //used for file path

app.use(busboy());

app.route('/upload')
    .post(function (req, res, next) {
        var fstream;
        req.pipe(req.busboy);
        var fileID = uuid.v4();
        fileID = fileID + '.3gp'
        var username = null;
        var description = null;
        var send_time = null;

        req.busboy.on('field', function (key, value){
        	console.log('---');
        	console.log(key);
        	console.log(value);
        	console.log('---');
        	if (key === 'username'){
        		username = value;
        		if (description!=null && send_time!=null){
					db.run("INSERT INTO Recording (name,file,description,send_time,seen) VALUES ('" + username + "','" + fileID + "','" + description + "','" + send_time + "',0);");
        		}
        	}
        	if (key ==='description'){
        		description = value;
        		if (username!=null && send_time!=null){
					db.run("INSERT INTO Recording (name,file,description,send_time,seen) VALUES ('" + username + "','" + fileID + "','" + description + "','" + send_time + "',0);");
        		}
        	}
        	if (key ==='send_time'){
        		send_time = value;
        		if (username!=null && description!=null){
					db.run("INSERT INTO Recording (name,file,description,send_time,seen) VALUES ('" + username + "','" + fileID + "','" + description + "','" + send_time + "',0);");
        		}
        	}
        	//store username : fileID into a database table

        });

        req.busboy.on('file', function (fieldname, file, filename) {
            console.log("Uploading: " + filename);

            //Path where file will be uploaded
            //fstream = fs.createWriteStream(__dirname + '/public/uploads/' + filename);
            fstream = fs.createWriteStream(__dirname + '/public/uploads/' + fileID);
            file.pipe(fstream);
            fstream.on('close', function () {    
                console.log("Upload Finished of " + filename);              
                //res.redirect('back');   
                resp = {};
                res.send(resp);        //send back empty json
            });
        });
    });


var sqlite3 = require("sqlite3").verbose();
var db = new sqlite3.Database(file);

app.use(express.static('public'));
// parse application/x-www-form-urlencoded
app.use(bodyParser.urlencoded({ extended: false }))

// parse application/json
app.use(bodyParser.json())

db.serialize(function() {
  if(!exists) {
    db.run("CREATE TABLE Practice (name TEXT, piano_time REAL, total_time REAL, sound_summary TEXT, start_time TEXT, end_time TEXT, key_count TEXT)");
    db.run("CREATE TABLE TeacherStudent (teacher TEXT, student TEXT)");
    db.run("CREATE TABLE Recording (name TEXT, file TEXT, description TEXT, send_time TEXT, seen INTEGER)");
  }
});

//db.run("INSERT INTO Recording (name,file,description,send_time,seen) VALUES ('charlie', 'file5.3gp', 'testing5', '2016-03-24 02:07:56', 0)")
//db.run("INSERT INTO Recording (name,file,description,send_time,seen) VALUES ('charlie', 'file6.3gp', 'testing6', '2016-03-25 02:09:46', 0)")

// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('bob', 19000.00, 20000.00, '{}', '2016-03-11 02:07:56', '2016-03-11 03:54:08');");
// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('charlie', 1700.00, 2600.00, '{}', '2016-03-09 11:07:56', '2016-03-09 11:54:08');");
// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('charlie', 3060.00, 10039.00, '{}', '2016-03-10 11:07:56', '2016-03-10 11:54:08');");
// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('charlie', 583.00, 1899.00, '{}', '2016-03-11 11:07:56', '2016-03-11 11:54:08');");
// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('charlie', 6749.00, 13133.00, '{}', '2016-03-12 11:07:56', '2016-03-12 11:54:08');");
// db.run("INSERT INTO Practice (name,piano_time,total_time, sound_summary, start_time, end_time) VALUES ('charlie', 4000.00, 6000.00, '{}', '2016-03-13 11:07:56', '2016-03-13 11:54:08');");

// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('alvin', 5000.00, 8000.00);");

// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('yuan', 2000.00, 3000.00);");
// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('yuan', 6000.00, 9000.00);");

// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('matt', 200.00, 5000.00);");
// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('matt', 50.00, 100.00);");
// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('matt', 5.00, 22.00);");
// db.run("INSERT INTO Practice (name,piano_time,total_time) VALUES ('matt', 40.00, 68.00);");


db.each("SELECT teacher, student FROM TeacherStudent", function(err, row) {
    console.log(row.teacher + "-" + row.student);
});

db.each("SELECT name,piano_time,total_time FROM Practice", function(err, row) {
    console.log(row.name + ": " + row.piano_time + "," + row.total_time);
});

db.each("SELECT name,file,description,send_time,seen FROM Recording", function(err,row){
	console.log(row.name + ": " + row.file + ": " + row.description + ": " + row.send_time + ": " + row.seen);
});


app.get('/teacher', function(req, res){
  res.sendFile(__dirname + '/teacher_view.html');
});

app.get('/student_view', function(req, res){
  res.sendFile(__dirname + '/teacher_student_view.html');
});


app.post('/', function(request, response){
  var username = request.body.username;
  var playtime = request.body.playtime;
  var totaltime = request.body.totaltime;
  var soundSummary = request.body.soundSummary;
  var soundSummaryString = JSON.stringify(soundSummary);
  var starttime = request.body.starttime;
  var endtime = request.body.endtime;
  var pianokeycount = request.body.pianokeycount;
  var pianokeycountString = JSON.stringify(pianokeycount);

  console.log(request.body);      

  //insert practice session data into db
  db.run("INSERT INTO Practice (name,piano_time,total_time,sound_summary,start_time,end_time,key_count) VALUES ('" + username + "'," + playtime + "," + totaltime + ",'" + soundSummaryString + "','" + starttime + "','" + endtime + "','" + pianokeycountString + "');");

  response.send(request.body);    // echo the result back
});

app.post('/add', function(request, response){
	var teachername = request.body.teachername;
	var studentname = request.body.studentname;


	db.run("INSERT INTO TeacherStudent (teacher,student) VALUES ('" + teachername + "','" + studentname + "');");
	console.log(request.body);
	response.send(request.body);
});


app.post('/getstudents', function(request, response){
	var teacher = request.body.teachername;
	resp = {};
	students = [];
	not_seen = [];
	db.serialize(function(){
		db.each("SELECT COUNT(DISTINCT student) FROM TeacherStudent WHERE teacher='" + teacher +"'", function(err,row){
			var n_students = parseInt(row['COUNT(DISTINCT student)']);
			db.serialize(function(){
				db.each("SELECT DISTINCT student FROM TeacherStudent WHERE teacher='" + teacher +"'", function(err, row) {
					var this_student = row.student;
					db.serialize(function(){
						db.each("SELECT COUNT(seen) FROM Recording WHERE name='" + this_student + "' AND seen=0", function(err, row){
							var n_not_seen = parseInt(row['COUNT(seen)']);
							students.push(this_student);
							not_seen.push(n_not_seen);
							//console.log(students.length);
							if (students.length == n_students){
								//console.log(students);
								resp['students'] = students;
								resp['not_seen'] = not_seen;
								response.send(resp);						
							}
						});
					});
				});
			});
		});
	});
});


app.post('/studentrecordings', function(request, response){
	var this_student = request.body.student;
	resp = {};
	recordings = [];
	descriptions = [];
	send_times = [];
	seen_array = [];
	db.serialize(function(){
		db.each("SELECT COUNT(name) FROM Recording WHERE name='" + this_student + "' ", function(err,row){
			var n_recordings = parseInt(row['COUNT(name)']);
			if (n_recordings == 0){
				resp['recordings'] = recordings;
				resp['descriptions'] = descriptions;
				resp['send_times'] = send_times;
				resp['seen_array'] = seen_array;
				response.send(resp);		
			}
			console.log('---recordings---');
			console.log(n_recordings);
			db.serialize(function(){
				db.each("SELECT name,file,description,send_time,seen FROM Recording WHERE name='" + this_student + "'", function(err, row){
					//console.log("pianotime: " + row.piano_time);
					recordings.push(row.file);
					descriptions.push(row.description);
					send_times.push(row.send_time);
					seen_array.push(row.seen);
					console.log(recordings.length);

					if (recordings.length == n_recordings){
						resp['recordings'] = recordings;
						resp['descriptions'] = descriptions;
						resp['send_times'] = send_times;
						resp['seen_array'] = seen_array;
						//console.log(resp);
						console.log('--send resp recording--')
					    response.send(resp);
					}
				});				
			});
		});
	});
});


app.post('/updateseen', function(request, response){
	var filename = request.body.file;
	db.run("UPDATE Recording SET seen=1 WHERE file='" + filename + "';");
	console.log('updated seen for ' + filename);
	var resp = {};
	response.send(resp);
});


app.post('/student', function(request, response){
	var this_student = request.body.student;
	sessions = [];
	resp = {};
	db.serialize(function(){
		db.each("SELECT COUNT(name) FROM Practice WHERE start_time > '2015-12-14 11:05:52' AND name='" + this_student + "' ", function(err,row){
			var n_sessions = parseInt(row['COUNT(name)']);
			console.log('---student---');
			console.log(n_sessions);
			if (n_sessions == 0){
				resp['sessions'] = sessions;
				response.send(resp);		
			}
			db.serialize(function(){
				db.each("SELECT name,piano_time,total_time,sound_summary,start_time,end_time,key_count FROM Practice WHERE start_time > '2015-12-14 11:05:52' AND name='" + this_student + "'", function(err, row){
					//console.log("pianotime: " + row.piano_time);
					session = {};
					session['piano_time'] = row.piano_time;
					session['total_time'] = row.total_time;
					session['sound_summary'] = JSON.parse(row.sound_summary);
					session['start_time'] = row.start_time;
					session['end_time'] = row.end_time;
					session['key_count'] = row.key_count;

					sessions.push(session);
					console.log(sessions.length);
					if (sessions.length == n_sessions){
						resp['sessions'] = sessions;    
						//console.log(resp);
						console.log('send resp student');
					    response.send(resp);
					}
				});				
			});
		});
	});
});


app.post('/reqinfo', function(request, response){
	var teacher = request.body.teachername;


	resp = {};
	students = [];	
	db.serialize(function() {
	db.each("SELECT COUNT(DISTINCT student) FROM TeacherStudent WHERE teacher='" + teacher +"'", function(err,row){
		console.log(parseInt(row['COUNT(DISTINCT student)']));
		var n_students = parseInt(row['COUNT(DISTINCT student)']);

		db.serialize(function(){
		db.each("SELECT DISTINCT student FROM TeacherStudent WHERE teacher='" + teacher +"'", function(err, row) {
			var this_student = row.student;

			db.serialize(function(){
			db.each("SELECT COUNT(name) FROM Practice WHERE name='" + this_student + "'", function(err,row){
				var n_sessions = parseInt(row['COUNT(name)']);
				console.log("n_sessions: " + n_sessions);
				console.log("student: " + this_student);
				if (n_sessions == 0){
					n_students -= 1; //student has no practice sessions
				}
				sessions = [];
				db.serialize(function(){
				db.each("SELECT name,piano_time,total_time,sound_summary FROM Practice WHERE name='" + this_student + "'", function(err, row){
					//console.log("pianotime: " + row.piano_time);
					session = {};
					session['piano_time'] = row.piano_time;
					session['total_time'] = row.total_time;
					session['sound_summary'] = JSON.parse(row.sound_summary);
					sessions.push(session);
					if (sessions.length == n_sessions){
						console.log("n_students:" + n_students);
						students.push(this_student);
						resp[this_student] = sessions;	
						sessions = [];	    
					    resp['students'] = students;
					    console.log(JSON.stringify(resp));
					    console.log(students.length);
					    if (students.length == n_students){
					    	console.log(JSON.stringify(resp));
					    	response.send(resp);
					    }
					}
				});
				});

			});
			});
		});
		});

	});
	});
});

http.listen(3000, '0.0.0.0');