# Cheese Calendar Parser
This project is a small Clojure rewrite of some Ruby code which parses scraped data from a website, for shift scheduling, for my partner's work, in a cheese shop.

## Objectives
There were basically no new functional requirements over and above the Ruby version of this process. This was purely an exercise to reimplement the process in a language I was less familiar with, to see how different the code was when trying to write it as idiomatically as possible.

## Ruby implementation
The previous implementation (shown below) was pretty straightforward Ruby code (code for writing the `.ics` file is ommitted).

```ruby
require './ics_writer'

# Parse data scraped from web site (via cheese_scraper.scpt) and output to STDOUT as .ics format

def parse_datetime(datestr)
  datetime = Time.parse(datestr)
  return datetime.to_time.utc
end

def parse_rota_date_line(line)
  components = line.split(" ")
  start_time = components[0..4].join(' ')
  end_time =  components[5..9].join(' ')
  return [start_time, end_time].map { |e| parse_datetime(e) }
end

def has_error(data_lines)
  return data_lines.any? { |x| /error/i.match(x) }
end

def parse_file(input_file)
  STDERR.puts "Reading file #{input_file}"
  raw_shift_data = File.read(input_file)
  
  if has_error(raw_shift_data.lines)
    STDERR.puts "Error loading shift page - exiting"
    exit(1)
  end
  
  start_end_times = []
  shift_count = 0
  right_person = false
  raw_shift_data.lines.each do |l|
    if /^\d\d:\d\d/.match(l) and right_person # Lines that start with a time like 11:00
      start_end_times.append(parse_rota_date_line(l))
      shift_count += 1
    elsif /^#{$target_name}$/i.match(l) or (/^Shift details$/i.match(l) and right_person)
      right_person = true
    else
      right_person = false
    end
  end

  if shift_count == 0
    STDERR.puts "Aborting - no shifts"
    exit(1)
  end
  
  STDERR.puts "Processed #{shift_count} shift(s) for #{$target_name}"
  puts ics_for_events(start_end_times)
end

$target_name = ARGV[0]
parse_file(ARGV[1])

```

### Ruby Process
The end-to-end process using the Ruby implementaton was as follows:
1. Applescript interaction with web site
   1. Open the relevant web page in Safari
   2. Detect if we get kicked back to a login page, and if-so, login using the stored credentials
   3. Save the text of the web page to a text file in the working directory
2. Script runs to process the saved text content and output as a `.ics` file
   1. Identify the relevant lines in the saved web content, looking for shifts for the correct person, and start and end dates and times for each shift
   2. For each shift identified, add these to a `.ics` file using a simple template
3. Sync the output `.ics` file to an online location. (This can then be subscribed to by calendar software.)

## Clojure implementation
The Clojure implemetation further splits the process out into further tasks.
1. *(Unchanged) Applescript interaction with web site*
   1. *Open the relevant web page in Safari*
   2. *Detect if we get kicked back to a login page, and if-so, login using the stored credentials*
   3. *Save the text of the web page to a text file in the working directory*
2. Script runs to process the saved text content and **output to an intermediate format**
   1. Identify the relevant lines in the saved web content, looking for shifts for the correct person, and start and end dates and times for each shift
   2. Produce a simple text file with a pair of start date-time / end date-time, one-line per-shift,
3. Read the text file with the start/end dates/times and use that to output the final `.ics` file, using templates
4. *(Unchanged) Sync the output `.ics` file to an online location. (This can then be subscribed to by calendar software.)*

### Key differences vs Ruby implemetation
#### Splitting out further
The Clojure code seemed to lend itself to being more finely modularised than felt natural in the Ruby implementation. This was done with the thought of being able to write other small scripts which just produce the intermediate output format (described below) and can then reuse a more generic transformer from that format to the final `.ics` file. Writing Clojure, with smaller self-contained functions, also seemed to make me look for other obvious separations at the module-level which were not so clear when writing Ruby.
#### State variables vs trampoline
The Ruby code has a couple of state variables to keep track of which line was being parsed at any given time and what state the parsing was in. These were replaced in the Clojure implementation with a function which gets called via `trampoline`, tracking the state as it progresses through the lines by way of which function it calls on each 'bounce' of the trampoline.
    
```clojure
(defn only-shift-lines-for-target
  "Find `target` in `lines` then output subsequent lines until a line is
  encountered which should not be output."
  [target lines]
  (letfn [(find-target [[line & rest-lines] shift-lines]
            (if line
              (if (= line target)
               #(found-target rest-lines shift-lines)
               #(find-target rest-lines shift-lines))
              ;; Exit point when `line` is nil: return shift-lines
              shift-lines))
          (found-target [[line & rest-lines] shift-lines]
            (if (= line "Shift details")
              #(found-target rest-lines shift-lines)
              (if (is-shift-line line)
                #(found-target rest-lines (conj shift-lines line))
                #(find-target rest-lines shift-lines))))]
    (trampoline find-target lines [])))
```

To me this has a slight readability penalty, but is a cleaner way of representing the state of the parsing.
The first approach I took here was closer to the Ruby implementation, and involved passing in two state variables with each call to the function which parsed an individual line. There was a separate function for parsing an individual line with the rather convoluted parameters: `[right-person output-line target line]`. This seemed to be mixing concerns in the required parameters, and the `trampoline` approach cleaned things up.
#### Code size
I was a little surprised how much more code the Clojure implemetation was vs the Ruby one. I think this may be as there are some remaining Clojure idioms I have missed. That said, coming to this code again 'cold' after a few months, I think the readability holds up better than the Ruby implemetation.
## Dates and Time Zones
As always, the date parsing was a little fiddly. The web content uses the local timezone from the browser, and the `.ics` format seems happiest with UTC datetimes. The conversions between these are handled in the `ics-datetimes` module. The `tick.core` library is used, as it is available in Babashka, and provided a relatively clean API for the required conversions.
There likely exists a very narrow timing-related bug where the data is read from the web site in one time zone (e.g., BST) and subsequently the intermediate data is written to the `.ics` at a time when the time zone has ticked over to GMT, but the code assumes that the current machine timezone is the same as that used to read the data in the first place. This could be mitigated by writing timezones with the saved intermediate date-times, and parsing those when they are subsequently read in by the next step in the process.

## Security
The credentials for login, and which URL use are held insecurely in plain-text files. These are excluded from version control. The password is not reused elsewhere. A more robust implementation would use some secrets store (in my case, likely macOS keychain). As the process is always run manually anyway, being prompted to unlock a credential store would not be in itself problematic.

## Babashka vs Clojure
This was written using [Babashka](https://babashka.org) instead of plain Clojure. Short-lived scripts like this feel like a better fit for Babashka vs full Clojure.

## Other take-aways
Also while doing this, I was learning the [`just`](https://just.systems) job scheduling system. I previously used [`rake`](https://github.com/ruby/rake), typically used for Ruby for running tasks and setting up dependencies. `just` is a more language agnostic job scheduling tool. The syntax for it is a little idiosyncratic, and I think if I was doing this again, I would use [Babaskha tasks](https://book.babashka.org/#tasks) to do the job instead, which would keep it more consistent across the whole end-to-end process.

## AI Usage
No AI tooling was used for this particular project.

