set quiet

target_name := `cat target.txt`
working_dir := 'working'
saved_rota_data_file := join(working_dir, 'saved_rota_data.txt')
intermediate_file := join(working_dir, 'shifts_intermediate.txt')
shift_calendar_file := join(working_dir, 'shifts.ics')
shift_end_calendar_file := join(working_dir, 'shift-ends.ics')

temp_web_dir := '~/Online/dircon/temp'
calendar_file_in_web_dir := join(temp_web_dir, 'cheese.ics')
calendar_end_file_in_web_dir := join(temp_web_dir, 'cheese-ends.ics')

# Get latest data from rota site and post online as .ics
cheese:
  just get_rota_data
  just parse_shifts_to_intermediate
  just intermediate_to_ics
  just put_ics_online

# Get latest data from rota site and save to .txt file
get_rota_data:
  echo "`date` starting (Babashka)..."
  osascript cheese_scraper.applescript > {{saved_rota_data_file}}

# Parse saved .txt file from web into dates and times only
parse_shifts_to_intermediate:
  bb -m shift-data/read-and-write '{{target_name}}' \
  {{saved_rota_data_file}} > {{intermediate_file}}
	
# Parse saved .txt file and write out as a .ics
intermediate_to_ics:
  fish -c 'test -s {{intermediate_file}}'
  bb -m ics/write-event-entries {{intermediate_file}} > {{shift_calendar_file}}
  bb -m ics/write-event-end-entries {{intermediate_file}} > {{shift_end_calendar_file}}

# Move & put latest .ics online
put_ics_online:
  cp -f {{shift_calendar_file}} {{calendar_file_in_web_dir}}
  cp -f {{shift_end_calendar_file}} {{calendar_end_file_in_web_dir}}
  (cd {{temp_web_dir}} && just mirror)
  echo OK - `grep 'BEGIN:VEVENT' {{calendar_file_in_web_dir}} | wc -l` shifts uploaded.
