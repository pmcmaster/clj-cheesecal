
on keypressDelay()
	delay (random number from 0.05 to 0.3)
end keypressDelay

on delayForPageLoad()
	delay (random number from 6 to 10)
end delayForPageLoad

on safariIsActive()
	tell application "System Events" to set frontProcess to name of first process where it is frontmost
	return frontProcess is "Safari"
end safariIsActive

on typeText(someText)
	repeat with eachChar in characters of someText
		if not my safariIsActive() then
			log "Expected Safari to be active"
			error number -1
		end if
		tell application "System Events" to keystroke eachChar
		my keypressDelay()
	end repeat
end typeText

on getCredentials()
	set credsFile to (the POSIX path of (path to home folder)) & "Code/clj-cheese-cal/creds.txt"
	set credsLines to paragraphs of (read credsFile)
	return credsLines
end getCredentials

on getURL()
        -- URL file just contains the URL for the page showing shift data
        -- This redirects to a login page in the case of not being logged in
        set urlFile to (the POSIX path of (path to home folder)) & "Code/clj-cheese-cal/url.txt"
        set urlLines to paragraphs of (read urlFile)
        return first item of urlLines
end

on cloudflareFromPassword()
	tell application "System Events"
		repeat 4 times
			my keypressDelay()
			keystroke tab -- Navigates to Cloudflare button
		end repeat
		my keypressDelay()
		keystroke space -- Ticks the box on the Cloudflare button
		my delayForPageLoad()
	end tell
end cloudflareFromPassword

on login()
	set credentials to my getCredentials()
	set username to first item of credentials
	set passwordForLogin to last item of credentials
	tell application "Safari" to activate
	tell application "System Events"
		keystroke tab
		log "Logging in as " & username
		my typeText(username)
		keystroke tab
		my keypressDelay()
		my typeText(passwordForLogin)
		my cloudflareFromPassword()
		keystroke tab -- Focus login button
		keystroke return -- triggers login button
	end tell
	log "Waiting for login"
	my delayForPageLoad()
end login

on openShiftsPage()
	log "Trying to open shifts page"
	tell application "Safari" to make new document with properties {URL: my getURL()}
	my delayForPageLoad()
	tell application "Safari"
		if (URL of document 1 as text) starts with "https://login" then
			log "Not logged in while trying to access rota page - logging in"
			my login()
		else
			log "Already logged in"
		end if
	end tell
end openShiftsPage

on fromDate()
	return do shell script "date -v-2d '+%d/%m/%Y'"
end fromDate

on toDate()
	return do shell script "date -v+4w '+%d/%m/%Y'"
end toDate

on dateRange()
	return my fromDate() & "-" & my toDate()
end dateRange

on setDateRangeToOneMonth()
	log "Setting focus on date filter"
	tell application "Safari"
		do JavaScript "document.getElementsByClassName('react-datepicker-wrapper')[0].getElementsByTagName('input')[0].focus()" in document 1
		delay 1
	end tell
	log "Typing date range filter"
	tell application "Safari" to activate
	tell application "System Events"
		keystroke "a" using command down
		key code 51 -- Delete keycode
	end tell
	set dateRangeStr to my dateRange()
	log ("Using date range '" & dateRangeStr & "'")
	typeText(dateRangeStr)
	tell application "System Events"
		keystroke tab
	end tell
	delay 1
end setDateRangeToOneMonth

on outputShiftDetails()
	tell application "Safari"
		set shiftDetails to text of document 1 as text
	end tell
	return shiftDetails
end outputShiftDetails

on closeSafariWindow()
	tell application "Safari" to close document 1
end closeSafariWindow

my openShiftsPage()
my setDateRangeToOneMonth()
set shiftDetails to my outputShiftDetails()
my closeSafariWindow()

return shiftDetails



