#
# Log monitoring suite for the Egraphs server project. It reads a particular format of log line
# by line, and sends e-mails and alerts based on events that occur in the logs. For usage
# of these libraries see the adjoining executable monitor.py, or the tests in log_monitor_tests.py.
#
# Generally you should only need to use the LogMonitor class with an ErrorLogListener.
#
import re
import copy
import collections
import threading
import smtplib
import traceback
import time

from email.mime.text import MIMEText

# Default of who receives e-mail. Can be overridden in the LogMonitor or ErrorLogListener.
mail_recipients = ['Erem Boto <erem@egraphs.com>', 'William Chan <will@egraphs.com>']

# Who the e-mails come from
system_email_address = "Egraphs Monitoring <monitoring@egraphs.com>"


#
# Mailers
#
class ConsoleMailer (object):
    """ Prints any incoming e-mails to the console. """

    def send(self, from_addr, to_addrs, mail):
        """ See SMTPMailer for description of this method. """
        print mail.as_string()

class SMTPMailer (object):
    """
    Sends incoming e-mails using the SMTP server located at localhost. Fails gloriously
    if localhost lacks an SMTP server. This is not a problem on EC2, but is on local machines.
    """

    def send(self, from_addr, to_addrs, mail):
        """ Sends an e-mail.

        Arguments:
        from_addr -- the string email address that is sending this. e.g. "monitoring@egraphs.com"
        to_addrs  -- list of string email addresses to receive the email.
        mail -- the MIMEText to send
        """
        s = smtplib.SMTP('localhost')
        s.sendmail(from_addr, to_addrs, mail.as_string())
        s.quit()

#
# LogListeners
#
        
class ErrorLogListener (object):
    """
    LogListener that keys in on ERROR priority levels in the logs and sends e-mails to the right
    people.
    """
    
    def __init__(self, mailer=SMTPMailer(), extra_lines_delay=3.0, mail_recipients=mail_recipients):
        """ Initializer.

        Arguments:
        mailer -- the Mailer implementation to use (see default for reference)
        extra_lines_delay -- Frequently a bunch of stack trace information comes in immediately
            after the initial error line. This value dictates the number of seconds to wait for
            the rest of the information before acting upon it by notifying the server ops.
        mail_recipients -- list of e-mail addresses to which to send error e-mails.
        """
        self._mailer = mailer
        self._extra_lines_delay = extra_lines_delay
        self._mail_recipients = mail_recipients

    def line_was_read(self, line, log_monitor):
        """ Receives a read and parsed line from the LogMonitor. """
        priority = line.priority
        context = line.context
        if priority in ["ERROR", "WARN"] and context and context != "<No context>":
            this_context_only_filter = lambda next_line: next_line.context == context
            this_context_lines = log_monitor.filter_lines(this_context_only_filter)

            # Wait for a few more lines of log data, so we can capture the stack trace
            def finish(extra_lines):
                this_context_lines.extend(extra_lines)
                # Format the relevant lines of log data
                format_context_line = lambda line: "%s %s\n" % (line.timestamp,
                                                                 line.message)

                context_lines_str = ''.join(
                    [format_context_line(line) for line in this_context_lines]
                )

                # Prepare the email
                from_addr = system_email_address
                to_addrs = self._mail_recipients
                msg = MIMEText(
                    error_email_text % {'context':context,
                                        'context_logs': context_lines_str}
                )
                msg['Subject'] = "%s: Error during process \"%s\"" % (log_monitor.service_name(),
                                                                      context)
                msg['From'] = system_email_address
                msg['To'] = ",".join(to_addrs)

                # Send the email
                self._mailer.send(from_addr, to_addrs, msg)

            # We'll wait for the next 100 lines (possible a terribly long stack trace)
            # Or a 3s timeout to make sure we get all information.
            log_monitor.wait_for_lines(
                100,
                finish,
                timeout_s=self._extra_lines_delay,
                line_filter=this_context_only_filter
            )
    
class TimedLogListener (object):
    """
    LogListener that performs an action after EITHER (a) enough log lines that pass a particular
    filter have been read, or (b) more than a specified amount of time elapsed waiting for (a) to
    be satisfied.
    """
    def __init__(self, num_lines_to_await, timeout_s, on_wait_over, log_monitor, line_filter):
        """ Initializer.

        Arguments:
        num_lines_to_await -- number of filter-passing lines to await before executing on_wait_over
        timeout_s -- number of seconds to wait for the LogMonitor to pass num_lines_to_await
            filter-passing lines before calling it quits and executing on_wait_over
        on_wait_over -- the function to call when either a timeout occurred or enough filter
            -passing LogLines were read. The function should accept a single list of LogLines.
        log_monitor -- the LogMonitor that will be passing lines to this instance.
        line_filter -- a function that takes a LogLine and returns either True or False. Only
            LogLines that return true will be counted towards meeting the threshold of
            num_lines_to_await.
        """
        self._num_lines_to_await = num_lines_to_await
        self._on_wait_over = on_wait_over
        self._lines = []
        self._line_filter = line_filter
        self._log_monitor = log_monitor

        self._timer = threading.Timer(timeout_s, self._timeout)
        
    def start(self):
        """ Starts the timer, which is necessary for meeting threshold (b) for executing
        self._on_wait_over.
        """
        self._timer.start()

    def line_was_read(self, line, log_monitor):
        if self._line_filter(line):
            lines = self._lines
            lines.append(line)
            
            if len(lines) == self._num_lines_to_await:
                self._timer.cancel()
                log_monitor.remove_listener(self)

                self._on_wait_over(self._lines)

    def _timeout(self):
        """ Called when the timeout threshold is complete. """
        self._log_monitor.remove_listener(self)
        self._on_wait_over(self._lines)
        
class InactivityLogListener (object):
    """
    LogListener that only reports to administrators if it hasn't seen any lines of
    log data in a while. After instantiating it must be manually start()ed and stop()ped
    in order to behave as specced. See the tests for example usage.
    """
    def __init__(self,
                 threshold_s=(3600*3),
                 pinging_frequency_s=300.0,
                 mailer=SMTPMailer(),
                 service_name="<service_name>"):
        """ Initializer.

        Arguments:
        threshold_s -- the maximum number of seconds that can pass between lines of logged
            information before this class will notify system admins that it thinks something
            is wrong.
        pinging_frequency -- the frequency with which the class will come in and check to see
            how much time has passed since it last read a line.
        mailer -- the mail implementation (e.g. SMTPMailer) that should be used to route out
            mail.
        service_name -- the name of the service being monitored, for identification in the
            email.        
        """
        self._threshold_s = threshold_s
        self._pinging_frequency_s = pinging_frequency_s
        self._last_line_read_time = None
        self._mailer = mailer
        self._mail_recipients = mail_recipients
        self._service_name = service_name
        self._current_timer = None

    def start(self):
        """ Begins checking in with frequency self._pinging_frequency_s to see if more than
        self._threshold_s time has passed since the last line of logs came in.        

        """
        self._last_line_read_time = time.time()
        self._schedule_new_ping()

    def stop(self):
        """ Stops checking if self._threshold_s time has passed since the last log came in """
        self._current_timer and self._current_timer.cancel()

    def line_was_read(self, line, log_monitor):
        """ This method is common to all LogMonitors. """
        self._last_line_read_time = time.time()
        
    def _schedule_new_ping(self):
        timer = threading.Timer(self._pinging_frequency_s, self._check_time)
        timer.start()

        self._current_timer = timer
        
    def _check_time(self):
        time_since_last_line = time.time() - self._last_line_read_time

        if time_since_last_line > self._threshold_s:
            # Send the e-mail and reset things.
            self._send_inactivity_email()
            self._last_line_read_time = time.time()
            self._schedule_new_ping()
        else:
            self._schedule_new_ping()

    def _send_inactivity_email(self):
        from_addr = system_email_address
        to_addrs = self._mail_recipients

        msg = MIMEText(inactivity_email_text % {'threshold' : self._threshold_s})
        msg['From'] = from_addr
        msg['To'] = ','.join(to_addrs)
        msg['Subject'] = "%s: Nothing logged in %s seconds" % (self._service_name,
                                                               self._threshold_s)

        self._mailer.send(from_addr, to_addrs, msg)

#
# LogLine Parsers
#                           
class MetadataParser (object):
    """
    Reads the structured metadata component of our server logs and puts them into the data
    portions of the LogLine.
    """
    def __init__(self):
        """ Initializer """
        self._regex = re.compile(r"(.*),(.*),(.*) ~~> (.*)")

    def read(self, line_obj):
        match = self._regex.search(line_obj.all)
        if match:
            new_line_obj = copy.copy(line_obj)
            new_line_obj.timestamp = match.group(1)
            new_line_obj.priority = match.group(2).strip()
            new_line_obj.context = match.group(3).strip()
            new_line_obj.message = match.group(4)

            return new_line_obj
        else:
            return line_obj
        
#
# Core classes
#
class LogMonitor (object):
    """
    The monitoring system's core class. Monitors lines from a log source one by one.

    Usage:
      monitor = LogMonitor(service_name="Egraphs live")
      monitor.add_listener(log_monitor.ErrorLogListener()) # Or any other log listener

      log_lines = # get your source of log file lines here. This can be a file or bees app:tail

      for line in log_lines:
          monitor.read_with_error_recovery(line)
    """
    def __init__(self,
                 service_name="<service name>",
                 parsers=[MetadataParser()],
                 mail_recipients=mail_recipients,
                 mailer=SMTPMailer()):
        """ Initializer.

        Arguments:
        service_name -- name of the servie whose logs are being monitored. This is used for
            reporting via e-mail, in the case that this script is being used to monitor several
            servers concurrently.
        parsers -- list of objects with a method read(LogLine) that returns a transformed copy
            of the LogLine. You probably don't need to touch this unless you're trying to glean
            more information from the syntax of the logs than that which is already analyzed by
            ErrorLogListener.
        mail_recipients -- list of email addresses in string format. These will be used to send
            a "whoops!" e-mail if the monitoring system should stack trace during calls to
            read_with_error_recovery.
        mailer -- the mailer object that should be used to send e-mail by this monitor. It should
            have a single method send(from_addr, to_addrs, mail). See SMTPMailer for more info about
            those arguments.            
        """
        self._service_name = service_name
        self._parsers = parsers
        self._listeners = frozenset()
        self._mailer = mailer        
        self._mail_recipients = mail_recipients
        self._line_cache = collections.deque(maxlen=1000)    

    def service_name(self):
        """ Returns the service name. See __init__ method documentation. """
        return self._service_name
        
    def line_cache(self):
        """ Returns the deque used to cache the last thousand lines of logs """
        return self._line_cache

    def add_listener(self, log_listener):
        """ Adds a LogListener object. After this call, the LogListener will be notified
        whenever a new line from the log source has been read. The LogListener should have a
        method line_was_read(LogLine, LogMonitor). See ErrorLogListener for more info about the
        arguments.
        """
        self._listeners = self._listeners.union([log_listener])

    def remove_listener(self, to_remove):
        """ Removes a LogListener. After this call, the LogListener will no longer be notified
        whenever a new line form the log source has been read.
        """
        self._listeners = frozenset(
            {listener for listener in self._listeners if listener is not to_remove}
        )
        
    def read_with_error_recovery(self, line):
        """ Attempts to read a string from the logs. If an error occurs during any point it catches
        the error and emails self._mail_recipients a letter of apology.
        """
        try:
            self.read(line)
        except:
            error_description = traceback.format_exc()
            try:
                self._send_whoops_mail(error_description)
            except Exception as e:
                print "I tried so hard to report all my errors, but in the end I failed as I always do."
                traceback.print_exc()
                

    def read(self, line):
        """ Attempts to read a string from the logs.

        In contrast to read_with_error_recovery, this method will bubble up any exceptions
        that occur while processing the logs.
        """
        useParser = lambda nextLine, parser: parser.read(nextLine)

        # Let our parsers grab metadata from the line data
        logLine = reduce(useParser, self._parsers, LogLine(line))

        # Alert our listeners about the new line
        map(lambda listener: listener.line_was_read(logLine, self), self._listeners)

        # Append it to the cache
        self._line_cache.append(logLine)    

    def wait_for_lines(self,
                       number_of_lines,
                       on_wait_over,
                       timeout_s,
                       line_filter=(lambda line: True)):
        """ Uses a TimedLogListener to notify a process when more lines from the log file are
        available. See docs from TimedLogListener for more information about the arguments.
        """
        timed_listener = TimedLogListener(
            number_of_lines,
            timeout_s,
            on_wait_over,
            self,
            line_filter
        )
        self.add_listener(timed_listener)

        timed_listener.start()

    def filter_lines(self, the_filter):
        """ Returns a list of LogLines from the cache that match the_filter

        Arguments:
        the_filter -- function that takes a LogLine and returns True or False. Only LogLines
             that produced "True" will be returned.
        """
        return [line for line in self._line_cache if the_filter(line)]

    def _send_whoops_mail(self, error_string):
        from_addr = system_email_address
        to_addrs = self._mail_recipients

        msg = MIMEText(whoops_email_text % {'stack_trace': error_string})
        msg['From'] = from_addr
        msg['To'] = ','.join(to_addrs)
        msg['Subject'] = "%s: Error in log monitoring system" % self._service_name

        self._mailer.send(from_addr, to_addrs, msg)
        
        
    
class LogLine (object):
    """
    A single line from the logs. Parsers (e.g. MetadataParser) populate the data felds with
    information from the log file string. Listeners use the fully populated data fields to
    make decisions about whether to alert the developers or not.
    """
    def __init__(self,
                 all_contents,
                 message="",
                 timestamp=None,
                 priority=None,
                 context=None):
        """ Initializer.

        Arguments:
        all_contents -- the full string from the log files
        message -- all of the log file that _wasn't_ metadata
        timestamp -- metadata: the timestamp of the logfile
        priority -- metadata: the priority (e.g. ERROR, INFO) of the line as logged by the server.
        context -- metadata: the stack context identifying what process produced the log.
            startsRequest

        """
        self.all = all_contents
        self.message = message
        self.timestamp = timestamp,
        self.priority = priority
        self.context = context
        
    def __repr__(self):
        return "LogLine(%s,%s,%s,%s)" % (self.timestamp,
                                            self.priority,
                                            self.context,
                                            self.message)


#
# Email text
#

# This one gets sent when we see an ERROR priority in the log files.
error_email_text = """Errors were logged during the process identified as "%(context)s". I'll dump all logs associated with the process into this e-mail.

Thanks,

The Egraphs Monitoring System

==== BEGIN LOGS ASSOCIATED WITH CONTEXT "%(context)s" ====

%(context_logs)s
"""

# This one gets sent when the monitoring system produces an error itself.
whoops_email_text = """Whoops! Some kind of gnarly error happened while I was trying to monitor the logs. It's almost certainly Erem's fault. Talk to him about this, because I'm his baby and he's my delinquent father.

Thanks,

The Egraphs Monitoring System

==== BEGIN MONITORING SYSTEM STACK TRACE ====

%(stack_trace)s
"""
    
inactivity_email_text = """Hey Fellas,

Hey you know it's been a little over %(threshold)s seconds since I've heard a peep from the log file I'm supposed to be monitoring. Can you make sure it still exists, and that it's still getting updated?

Not a big deal...just a bit worried.

Thanks,

The Egraphs Monitoring System
"""
