var teacherApp = angular.module('teacherApp', ['ngStorage']);

teacherApp.controller('teacherAppCtrl', function ($scope, $localStorage, $sessionStorage) {

    $scope.months =  [
        "January", "February", "March",
        "April", "May", "June", "July",
        "August", "September", "October",
        "November", "December"
    ];

    google.charts.load('current', {packages: ['corechart', 'bar']});

	$scope.teacher_name = "";
	$scope.student_to_add = "";

	$scope.students = [];
    $scope.not_seen = [];

	$scope.student_info = {};
    $scope.wavesurfer = null;
    $scope.login_view = true;
    $scope.all_students_view = false;
    $scope.student_specific_view = false;
    $scope.sessions = [];
    $scope.this_week = [];
    $scope.seen_array = [];

    $scope.waves = null;
    $scope.recordings = null;

    $scope.descriptions = null;
    $scope.send_times = null;

    $scope.sliders = null;
    $scope.sliderValues = null;
    $scope.curr_student = null;

    $scope.get_students = function(){

        $scope.login_view = false;
        $scope.all_students_view = true;
        $.post("/getstudents",
        {
            teachername: $scope.teacher_name
        },
        function(data, status){
            $scope.students = data['students'];
            $scope.not_seen = data['not_seen'];
            $scope.$apply();
        });
    }


    function drawKeyCount(div_id){
        var key_count = $scope.sessions[i]['key_count'];
        //key_count = parseInt(key_count);
        key_count = JSON.parse(key_count);
        console.log(key_count);
        var data = new google.visualization.DataTable();
        data.addColumn('number', 'Key');
        data.addColumn('number', 'Count');

        for (i in key_count){
            console.log(key_count[i]);
            kc = parseFloat(key_count[i]);
            console.log(kc);
            data.addRow([parseFloat(i), {v: kc, f: kc.toFixed(6)}]); 
            //data.addRow([String(i), parseFloat(key_count[i])]);
        }

      var options = {
        title: 'Key Count of This Session',
        hAxis: {
          title: 'Key #',
          // format: 'h:mm a',
          // viewWindow: {
          //   min: [0, 30, 0],
          //   max: [17, 30, 0]
          // }
        },
        bar: {groupWidth: "100%"},
        colors: ['#1b9e77'],
        legend: {position: 'top', maxLines: 3},
        vAxis: {
          title: 'Count'
        }
      };
      var id_str = "kc_" + String(div_id);
      var chart = new google.visualization.ColumnChart(
        document.getElementById(id_str));

      chart.draw(data, options);

    }


    function drawBasic() {

          var data = new google.visualization.DataTable();
          data.addColumn('string', 'Date');
          data.addColumn('number', 'Minutes Practiced');

          dates = [];
          minutes = [];

          //console.log('---');
          for (i=6; i>=0; i--){
            var daysAgo = new Date();

            var day = daysAgo.getDate();
            var monthIndex = daysAgo.getMonth()+1;
            //console.log(monthIndex);
            var year = daysAgo.getFullYear();

            daysAgo = new Date(year + "-" + monthIndex + "-" + day + " 00:00:00");
            daysAgo.setDate(daysAgo.getDate() - i);
            //console.log(daysAgo);

            day = daysAgo.getDate();
            monthIndex = daysAgo.getMonth();
            year = daysAgo.getFullYear();

            dateString = String($scope.months[monthIndex] + " " + day + ", " + year);
            dates.push(dateString);
            minutes.push(0.0);
          }

          for (j in $scope.this_week){
            this_session = $scope.this_week[j];
            this_date = new Date(this_session['end_time']);
            //console.log(this_date);
            for (k=6; k>=0; k--){
                var daysAgo = new Date();
                var day = daysAgo.getDate();
                var monthIndex = daysAgo.getMonth()+1;
                var year = daysAgo.getFullYear();

                daysAgo = new Date(year + "-" + monthIndex + "-" + day + " 00:00:00");
                daysAgo.setDate(daysAgo.getDate() - k);

                day = daysAgo.getDate();
                monthIndex = daysAgo.getMonth()+1;
                year = daysAgo.getFullYear();
                //console.log(k);
                //console.log(daysAgo);
                daysAgoPlusOne = new Date(year + "-" + monthIndex + "-" + day + " 00:00:00");
                daysAgoPlusOne.setDate(daysAgoPlusOne.getDate()+1);
                if (this_date - daysAgo > 0 && this_date - daysAgoPlusOne < 0){
                    
                    // console.log('------');
                    // console.log(this_date);
                    // console.log(daysAgo);
                    // console.log(daysAgoPlusOne);
                    // console.log('------');

                    minutes[6-k] += this_session['piano_time']/60;
                    break;
                }
            }
          }

          // console.log(minutes);
          // console.log(dates);

          for (i in dates){
            data.addRow([dates[i], minutes[i]]);
          }

          // for (i in $scope.this_week){
          //   this_session = $scope.this_week[i];
          //   this_date = new Date(this_session['end_time']);
          //   dateString = String(this_date.getDate());
          //   data.addRow([{v:this_date, f:dateString}, this_session['piano_time']/60]);
          // }
          // data.addRows([
          //   [{v: [8, 0, 0], f: '8 am'}, 1],
          //   [{v: [9, 0, 0], f: '9 am'}, 2],
          //   [{v: [10, 0, 0], f:'10 am'}, 3],
          //   [{v: [11, 0, 0], f: '11 am'}, 4],
          //   [{v: [12, 0, 0], f: '12 pm'}, 5],
          //   [{v: [13, 0, 0], f: '1 pm'}, 6],
          //   [{v: [14, 0, 0], f: '2 pm'}, 7],
          //   [{v: [15, 0, 0], f: '3 pm'}, 8],
          //   [{v: [16, 0, 0], f: '4 pm'}, 9],
          //   [{v: [17, 0, 0], f: '5 pm'}, 10],
          // ]);

          var options = {
            title: 'Minutes Practiced This Week',
            legend: {position: 'top', maxLines: 3},
            hAxis: {
              title: 'Day',
              // format: 'h:mm a',
              // viewWindow: {
              //   min: [0, 30, 0],
              //   max: [17, 30, 0]
              // }
            },
            vAxis: {
              title: 'Minutes',
              minValue: 0,
              viewWindow: {
                min: 0
              }
            }
          };

          var chart = new google.visualization.ColumnChart(
            document.getElementById('chart_div'));

          chart.draw(data, options);
    }

    function generateRandomWaves(number){
        var arr = [];
        var max = 50;
        var min = -50;
        for (i=0; i<number; i++){
            arr.push(Math.floor(Math.random()*(max-min+1)+min));
        }
        return arr;
    }

    function isEmpty(obj) {

        // null and undefined are "empty"
        if (obj == null) return true;

        // Assume if it has a length property with a non-zero value
        // that that property is correct.
        if (obj.length > 0)    return false;
        if (obj.length === 0)  return true;

        // Otherwise, does it have any properties of its own?
        // Note that this doesn't handle
        // toString and valueOf enumeration bugs in IE < 9
        for (var key in obj) {
            if (hasOwnProperty.call(obj, key)) return false;
        }

        return true;
    }

    $scope.redirect_student = function(index){
        $scope.curr_student = $scope.students[index];


        $.post("/student", 
        {
            student: $scope.students[index]
        },
        function(data, status){
            $scope.sessions = data['sessions'].reverse();

            //graph weekly and monthly
            this_week_sessions = [];

            var oneWeekAgo = new Date();
            oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);

            var day = oneWeekAgo.getDate();
            var monthIndex = oneWeekAgo.getMonth()+1;
            var year = oneWeekAgo.getFullYear();

            oneWeekAgo = new Date(year + "-" + monthIndex + "-" + day + " 00:00:00");

            for (i in $scope.sessions){
                $scope.sessions[i]['percentage'] = parseFloat($scope.sessions[i]['piano_time'])*100/parseFloat($scope.sessions[i]['total_time']);
                $scope.sessions[i]['minutes'] = $scope.sessions[i]['total_time']/60;
                //console.log($scope.sessions[i]['end_time']);
                var practice_date = new Date($scope.sessions[i]['end_time']);
                if (practice_date - oneWeekAgo > 0){
                    this_week_sessions.push($scope.sessions[i]);
                }
                // for (i=0; i<$scope.sessions.length; i++){
                //     console.log($scope.sessions[i]['end_time']);
                //     $scope.sessions[i]['end_time'] = $scope.sessions[i]['end_time'].substr(0,$scope.sessions[i]['end_time'].length-3);
                // }
                // for (i=0; i<$scope.sessions.length; i++){
                //     $scope.sessions[i]['start_time'] = $scope.sessions[i]['start_time'].substr(0,$scope.sessions[i]['start_time'].length-3);
                // }
            }

            $scope.this_week = this_week_sessions;
            drawBasic();

            for (i=0; i<$scope.sessions.length; i++){
                //console.log($scope.sessions[i]['end_time']);
                $scope.sessions[i]['end_time'] = $scope.sessions[i]['end_time'].substr(0,$scope.sessions[i]['end_time'].length-3);
            }
            for (i=0; i<$scope.sessions.length; i++){
                $scope.sessions[i]['start_time'] = $scope.sessions[i]['start_time'].substr(0,$scope.sessions[i]['start_time'].length-3);
            }



            //update angular
            $scope.$apply();

            //must be done after angular update because of div id

            for (i in $scope.sessions){
                drawKeyCount(i);
            }


            console.log('/student sent response');

            $.post("/studentrecordings",
            {
                student: $scope.students[index]
            },
            function(data, status){
                $scope.recordings = data['recordings'].reverse();
                $scope.descriptions = data['descriptions'].reverse();
                $scope.send_times = data['send_times'].reverse();

                for (i=0; i<$scope.send_times.length; i++){
                    $scope.send_times[i] = $scope.send_times[i].substr(0,$scope.send_times[i].length-3);
                }
                $scope.seen_array = data['seen_array'].reverse();
                //test
                // var temp = $scope.recordings;
                // $scope.recordings = [];
                //$scope.recordings.push('recording.3gp');
                // $scope.recordings.push(temp[0]);
                console.log($scope.recordings);
                $scope.waves = [];
                $scope.sliders = [];
                //$scope.sliderValues = [];
                $scope.$apply();

                for (i=0; i<$scope.recordings.length; i++){
                    var wave = Object.create(WaveSurfer);
                    wave.init({
                        container: '#waveform_' + String(i),
                        waveColor: 'violet',
                        progressColor: 'purple'
                    });

                    var loadString = "uploads/" + $scope.recordings[i];
                    console.log(loadString);
                    wave.load(loadString);
                    // $scope.wavesurfer.on('ready', function () {
                    //     $scope.wavesurfer.play();
                    // });

                    $scope.waves.push(wave);



                    /* slider */

                    //var slider = document.querySelector('#slider_' + String(i));
                    //$scope.sliders.push(slider);

                    // slider.oninput = function () {
                    //   var zoomLevel = Number(slider.value);
                    //   console.log(zoomLevel);
                    //   wave.zoom(zoomLevel);
                    // };
                }
                console.log($scope.waves);
                // for (i=0; i<$scope.sliders.length; i++){
                //     var s = $scope.sliders[i];
                //     s.oninput = function(){
                //         var zoomLevel = Number(s.value);
                //         $scope.waves[i].zoom(zoomLevel);
                //     }
                // }

            }); 


            //show practice session waveforms
            for (j=0; j<$scope.sessions.length; j++){
                var idString = "wave_" + String(j);
                var c = document.getElementById(idString);
                console.log(c);
                console.log('creating waveform:')
                summ = $scope.sessions[j].sound_summary;
                if (isEmpty(summ)){
                    console.log("NULL");
                    var seconds = $scope.sessions[j].total_time;
                    summ = generateRandomWaves(seconds*4);
                }
                var max_summ = Math.max.apply(Math, summ);
                new_data = [];
                for (k=0; k<summ.length; k++){
                    new_data.push(summ[k]/max_summ);
                }
                var waveform = new Waveform({
                    container: document.getElementById(idString),
                    //data: $scope.student_info[$scope.students[i]][j].sound_summary,
                    //data:[0,1,0,1],
                    data: new_data,
                    innerColor: "#ff9933"
                });                          
            }
        });


        $scope.all_students_view = false;
        $scope.student_specific_view = true;
    }

    $scope.back = function(){
        if ($scope.waves!=null){
            for (i=0; i<$scope.waves.length; i++){
                if ($scope.waves[i]!=null){
                    if ($scope.waves[i].isPlaying()){
                        $scope.waves[i].stop();
                    }                
                }
            }            
        }

        $scope.playing = [];
        $scope.sessions = null;
        $scope.recordings = null;
        $scope.waves = null;
        $scope.sliders = null;
        $scope.sliderValues = null;
        //$scope.all_students_view = true;
        $scope.student_specific_view = false;  
        $scope.get_students();
    }

    $scope.overCactus = function(index){
        var str = String(index);
        var cactus = document.getElementById('cactus_' +str);
        cactus.src = "img/cactus_listening.png";
    }

    $scope.offCactus = function(index){
        var str = String(index);
        var cactus = document.getElementById('cactus_' +str);
        cactus.src = "img/cactus_waiting.png";
    }

	$scope.request_student_info = function() {
		$.post("/reqinfo",
        {
          teachername: $scope.teacher_name,
        },
        function(data,status){
            $scope.student_info = data;
            $scope.students = data['students'];
            $scope.$apply();

            for (i=0; i<$scope.students.length; i++){
                for (j=0; j<$scope.student_info[$scope.students[i]].length; j++){
                    var idString = String($scope.students[i]) + "_" + String(j);
                    var c = document.getElementById(idString);
                    console.log(c);
                    console.log('creating waveform:')
                    summ = $scope.student_info[$scope.students[i]][j].sound_summary;
                    var max_summ = Math.max.apply(Math, summ);
                    new_data = [];
                    for (k=0; k<summ.length; k++){
                        new_data.push(summ[k]/max_summ);
                    }
                    var waveform = new Waveform({
                        container: document.getElementById(idString),
                        //data: $scope.student_info[$scope.students[i]][j].sound_summary,
                        //data:[0,1,0,1],
                        data: new_data,
                        innerColor: "#ff9933"
                    });                           
                }
            }
            //console.log($scope.students);
            //console.log(JSON.stringify($scope.student_info));
            
        });


	}

	$scope.add_student = function(){
        $.post("/add",
        {
          teachername: $scope.teacher_name,
          studentname: $scope.student_to_add
        },
        function(data,status){
            alert("Added New Student: " + data.studentname);
            $scope.get_students();
        });
	}

    $scope.show_wave = function(){
        $scope.wavesurfer = WaveSurfer.create({
            container: '#waveform',
            waveColor: 'violet',
            progressColor: 'purple'
        });
        
        $scope.wavesurfer.load("uploads/recording.3gp");
        $scope.wavesurfer.on('ready', function () {
            $scope.wavesurfer.play();
        });

        var slider = document.querySelector('#slider');

        slider.oninput = function () {
          var zoomLevel = Number(slider.value);
          $scope.wavesurfer.zoom(zoomLevel);
        };
    }

    $scope.playPause = function(index){
        if ($scope.waves[index]!=null){
            $scope.waves[index].playPause();
        }

        var filename = $scope.recordings[index];
        if ($scope.seen_array[index]!=1){
            $scope.seen_array[index] = 1;
            $.post("/updateseen",
            {
                file: filename
            },
            function(data, status){

            });
        }
    }

    $scope.oninput = function(index){
        if ($scope.sliders[index]!=null){
            var zoomLevel = Number($scope.sliders[index].value);
            $scope.waves[index].zoom(zoomLevel);
        }
    }

});
