function initCal() {

	
	$('#calendar').cal({
		
		resources : {
			'15' : 'Aziza',
			'16' : 'Heath',
			'17' : 'Karen',
			'90' : 'Michelle'
		},
		
		allowresize		: true,
		allowmove		: true,
		allowselect		: true,
		allowremove		: true,
		allownotesedit	: true,
		daytimestart:	'06:00:00',
		daytimeend: 	'22:00:00',
		
		eventselect : function( uid ){
			console.log( 'Selected event: '+uid );
		},
		
		eventremove : function( uid ){
			console.log( 'Removed event: '+uid );
		},
		
		
		eventnotesedit : function( uid ){
			console.log( 'Edited Notes for event: '+uid );
		},
		
		// Load events as .ics
		events : //'http://staff.digitalfusion.co.nz.local/time/calendar/leave/'
		
		[
			{
				uid		: 1,
				begins	: $.cal.date().addDays(2).format('Y-m-d')+' 10:10:00',
				ends	: $.cal.date().addDays(2).format('Y-m-d')+' 12:00:00',
				color	: '#dddddd',
				resource: '90',
				title	: 'Done'
			},
			{
				uid		: 2,
				begins	: $.cal.date().addDays(2).format('Y-m-d')+' 12:15:00',
				ends	: $.cal.date().addDays(2).format('Y-m-d')+' 13:45:00',
				resource: '16',
				notes	: 'Keepin\' it real…\n\nMan.'
			},
			{
				uid		: 3,
				begins	: $.cal.date().addDays(2).format('Y-m-d')+' 10:30:00',
				ends	: $.cal.date().addDays(2).format('Y-m-d')+' 12:15:00',
				color	: 'rgb( 90, 0, 0 )',
				resource: '16',
				notes	: 'The cake is a lie.'
			},
			{
				uid		: 4,
				begins	: $.cal.date().addDays(3).format('Y-m-d')+' 14:15:00',
				ends	: $.cal.date().addDays(3).format('Y-m-d')+' 16:30:00',
				resource: '17',
				notes	: 'An <example> event…'
			},
			{
				uid		: 5,
				begins	: $.cal.date().addDays(4).format('Y-m-d')+' 11:30:00',
				ends	: $.cal.date().addDays(4).format('Y-m-d')+' 12:30:00',
				color	: '#990066',
				notes	: 'The big game'
			},
			{
				uid		: 6,
				begins	: $.cal.date().addDays(0).format('Y-m-d')+' 12:30:00',
				ends	: $.cal.date().addDays(2).format('Y-m-d')+' 8:45:00',
				resource: '17',
				notes	: 'Good-O'
			}
		],
		masktimelabel: {
	        '00' : 'g:i a'
	    }
		
	});
	
}

