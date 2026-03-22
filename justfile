#set quiet

target_name := `cat target.txt`
saved_rota_data_file := 'working/saved_rota_data.txt'
shift_calendar_file := 'working/shifts.ics'
calendar_file_in_web_dir := '~/Online/dircon/temp/cheese.ics'
temp_web_dir := parent_dir(calendar_file_in_web_dir)

# Get latest data from rota site and post online as .ics
cheese:
  just get_rota_data
  just parse_data_to_ics
  just put_ics_online

# Get latest data from rota site and save to .txt file
get_rota_data:
  echo `date` starting...
  osascript cheese_scraper.applescript > {{saved_rota_data_file}}

# Parse saved .txt file and write out as a .ics
parse_data_to_ics:
  bb cheese_cal.clj '{{target_name}}' {{saved_rota_data_file}} > {{shift_calendar_file}}

# Move & put latest .ics online
put_ics_online:
  cp -f {{shift_calendar_file}} {{calendar_file_in_web_dir}}
  (cd {{temp_web_dir}} && just mirror)
  echo OK - `grep 'BEGIN:VEVENT' {{calendar_file_in_web_dir}} | wc -l` shifts uploaded.
