#!/usr/bin/python
import log_monitor
import copy
import time

from log_monitor import LogMonitor, LogLine, MetadataParser, ErrorLogListener, SMTPMailer, InactivityLogListener
from email.mime.text import MIMEText

line_without_metadata = "Herpy derp derp derp"
line_with_metadata = "03/29 18:25:07.100,INFO ,I has a context ~~> Hewwo dear "

#### Helper classes
class MockMailer (object):
    def __init__(self):
        self.sent = []

    def send(self, from_addr, to_addrs, mail):
        self.sent.append((from_addr, to_addrs, mail))

##
## Begin test cases
##
        
##############################################
print "\nInactivityLogListener: If no lines are read within the notification threshold it should send an email"
mockMailer = MockMailer()
service_name = "egraphs_test"
monitor = LogMonitor(service_name=service_name)
inactivityListener = InactivityLogListener(
  threshold_s=0.5,
  pinging_frequency_s=0.1,
  mailer=mockMailer,
  service_name=service_name
)

inactivityListener.start()
monitor.add_listener(inactivityListener)
time.sleep(0.6)
inactivityListener.stop()

assert len(mockMailer.sent) == 1

from_addr, to_addrs, msg = mockMailer.sent[0]

assert service_name in msg['Subject']
assert 'erem@egraphs.com' in msg['To']
assert 'will@egraphs.com' in msg['To']


##############################################
print "\nInactivityLogListener: If no lines are read within 2x the notification threshold, it should send 2 mails"
mockMailer = MockMailer()
service_name = "egraphs_test"
monitor = LogMonitor(service_name=service_name)
inactivityListener = InactivityLogListener(
  threshold_s=0.5,
  pinging_frequency_s=0.1,
  mailer=mockMailer,
  service_name=service_name
)

inactivityListener.start()
monitor.add_listener(inactivityListener)
time.sleep(1.1)
inactivityListener.stop()

assert len(mockMailer.sent) == 2


##############################################
print "\nInactivityLogListener: If lines are read within the threshold, it should not send e-mails"
mockMailer = MockMailer()
service_name = "egraphs_test"
monitor = LogMonitor(service_name=service_name)
inactivityListener = InactivityLogListener(
  threshold_s=0.5,
  pinging_frequency_s=0.1,
  mailer=mockMailer,
  service_name=service_name
)

inactivityListener.start()
monitor.add_listener(inactivityListener)

monitor.read("herp")
time.sleep(0.2)
monitor.read("herp")
time.sleep(0.2)
monitor.read("herp")
inactivityListener.stop()

assert len(mockMailer.sent) == 0


##############################################
print "\nErrorLogListener: It should parse a sample error log correctly"

mockMailer = MockMailer()        
errorLogListener = ErrorLogListener(mailer=mockMailer, extra_lines_delay=0.5)
service_name = "egraphs_test"

monitor = LogMonitor(service_name=service_name, mailer=MockMailer())
monitor.add_listener(errorLogListener)
f = open('error_example.log')

lines = f.readlines()
for line in lines:
    monitor.read(line)

time.sleep(1.0)

assert len(mockMailer.sent) == 6

first_from_addr, first_to_addrs, first_email = mockMailer.sent[0]

assert service_name in first_email['Subject']
assert "logStuffThenThrowException(XUkF6V.WWz7lx)" in first_email['Subject']
assert "erem@egraphs.com" in first_email['To']
assert "erem@egraphs.com" in first_to_addrs[0]
assert "will@egraphs.com" in first_email['To']
assert "will@egraphs.com" in first_to_addrs[1]


##############################################
print "\nLogMonitor._send_whoops_mail: It should send the correct mail text to the correct people."
mockMailer = MockMailer()
monitor = LogMonitor(mailer=mockMailer, service_name="test log")

error_message = "I fucked up, Johnny"
monitor._send_whoops_mail(error_message)

assert len(mockMailer.sent) == 1

from_addr, to_addrs, msg = mockMailer.sent[0]

assert "erem@egraphs.com" in ' '.join(to_addrs)
assert "erem@egraphs.com" in msg['To']
assert "will@egraphs.com" in ' '.join(to_addrs)
assert "will@egraphs.com" in msg['To']
assert "test log" in msg['Subject']


##############################################
print "\nLogMonitor.read_with_recovery: It should send a whoops mail if an error occurs during read."

class ListenerThatThrowsErrors (object):
    def line_was_read(self, line, log_monitor):
        raise Exception("I'm sorry Johnny...I screwed up big time")

mockMailer = MockMailer()
monitor = LogMonitor(mailer=mockMailer, service_name="test log")
monitor.add_listener(ListenerThatThrowsErrors())

monitor.read_with_error_recovery(line_without_metadata)

assert len(mockMailer.sent) == 1

_, _, msg = mockMailer.sent[0]

# Check that the stack trace and message are in the email
assert "log_monitor_tests.py" in msg.as_string()
assert "I'm sorry Johnny...I screwed up big time" in msg.as_string()

##############################################
print "\nLogMonitor.read: It should pass LogLines to the parsers and the parse result to Listeners."
class TestParser (object):
    def __init__(self):
        self.lines_that_were_read = []

    def read(self, line):
        newLine = copy.copy(line)
        newLine.context = "Test"
        
        self.lines_that_were_read.append(line)

        return newLine

class TestListener (object):
    def __init__(self):
        self.lines_that_were_received = []

    def line_was_read(self, line, log_monitor):
        self.lines_that_were_received.append(line)
    
testParser = TestParser()
testListener = TestListener()

monitor = LogMonitor(parsers=[testParser])
monitor.add_listener(testListener)

monitor.read(line_without_metadata)
assert len(testParser.lines_that_were_read) == 1
assert testParser.lines_that_were_read[0].all == line_without_metadata
assert len(testListener.lines_that_were_received) == 1
assert testListener.lines_that_were_received[0].all == line_without_metadata
assert testListener.lines_that_were_received[0].context == "Test"


##############################################
print "\nLogMonitor.read: It should ignore repeated lines."
testListener = TestListener()

monitor = LogMonitor(parsers=[])
monitor.add_listener(testListener)

monitor.read("Herp")
monitor.read("Herp")

assert len(testListener.lines_that_were_received) == 1

monitor.read("Derp")

assert len(testListener.lines_that_were_received) == 2


##############################################
print "\nLogMonitor.wait_for_lines: Reading the requisite number of lines.",
print "No timeout should occur."

lines_observed = []

monitor = LogMonitor(parsers=[testParser])
monitor.add_listener(testListener)
monitor.wait_for_lines(1, lambda lines: lines_observed.extend(lines), timeout_s=0.25)

assert len(monitor._listeners) == 2
monitor.read(line_without_metadata)

assert len(lines_observed) == 1
time.sleep(0.5)
assert len(lines_observed) == 1
assert lines_observed[0].all == line_without_metadata
assert len(monitor._listeners) == 1


##############################################
print "\nLogMonitor.wait_for_lines: Read an insufficient number of lines before the timeout.",
print "Timeout should occur and send the one line we got to the callback function."""
lines_observed = []

monitor = LogMonitor(parsers=[testParser])
monitor.add_listener(testListener)
monitor.wait_for_lines(2, lambda lines: lines_observed.extend(lines), timeout_s=0.25)
monitor.read(line_without_metadata)

assert len(lines_observed) == 0
assert len(monitor._listeners) == 2
time.sleep(0.5)
assert len(lines_observed) == 1
assert lines_observed[0].all == line_without_metadata
assert len(monitor._listeners) == 1


##############################################
print "\nLogMonitor.wait_for_lines: Only lines that pass the filter should count towards the total."

lines_observed = []
monitor.wait_for_lines(
    1,
    lambda lines: lines_observed.extend(lines),
    timeout_s=0.25,
    line_filter=lambda line: "herp" in (line.all)
)

assert len(monitor._listeners) == 2

# All of these reads should result in 0 lines observed because they didn't contain 'herp'
monitor.read("bla bla bla")
monitor.read("derp derp derp")
monitor.read(line_without_metadata)
assert len(lines_observed) == 0

# Now we read a line with herp, and that should satisfy the waiting process
monitor.read("You see I herp then I derp")
assert len(lines_observed) == 1


##############################################
print "\nMetadataParser: It should be able to parse the metadata before '~~>' is encountered."
metadata_parser = MetadataParser()
parsed_line_with_metadata = metadata_parser.read(LogLine(line_with_metadata))

assert parsed_line_with_metadata.timestamp == "03/29 18:25:07.100"
assert parsed_line_with_metadata.priority == "INFO"
assert parsed_line_with_metadata.context == "I has a context"
assert parsed_line_with_metadata.message == "Hewwo dear "
assert parsed_line_with_metadata.all == line_with_metadata


##############################################
print "\nMetadataParser: It should ignore lines that don't contain the '~~>' marker"
log_line_without_metadata = LogLine(line_without_metadata)
assert metadata_parser.read(log_line_without_metadata) == log_line_without_metadata


##############################################
print "\n\nAll local tests are complete. Press enter to continue tests _only_ if you're running from a machine with a valid smtp server at localhost. Otherwise the tests will fail."
a = raw_input("\033[00;32mContinue with server-only tests? (CTRL+C to end the tests): \033[00m")
print "SMTPMailer: It should send an email."
from_addr = 'monitoring@egraphs.com'
to_addrs = ['Erem Boto <erem@egraphs.com>', 'William Chan <will@egraphs.com>']
msg = MIMEText("As you were; this is just the monitoring testcase running.")
msg['Subject'] = "Monitoring testcase."
msg['From'] = 'monitoring@egraphs.com'
msg['To'] = ','.join(to_addrs)

# Send the email
mailer = SMTPMailer()
mailer.send(from_addr, to_addrs, msg)

