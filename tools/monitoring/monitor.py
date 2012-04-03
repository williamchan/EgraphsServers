#!/usr/bin/python

import filetail
import log_monitor
import sys
import traceback
import os.path

if len(sys.argv) < 2:
    print "Give me a log file to monitor, chief."
    sys.exit(1)
else:
    log_file = sys.argv[1]
    service_name = os.path.basename(log_file)
    monitor = log_monitor.LogMonitor(service_name=service_name)
    monitor.add_listener(log_monitor.ErrorLogListener())
    monitor.add_listener(log_monitor.InactivityLogListener(service_name=service_name))

    t = filetail.Tail(log_file, only_new=True)

    print "Beginning to monitor service \"%s\" (%s)" % (service_name, log_file)
    for line in t:
        monitor.read_with_error_recovery(line)
