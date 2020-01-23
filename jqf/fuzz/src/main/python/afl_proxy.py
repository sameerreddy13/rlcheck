# Copyright (c) 2017, University of California, Berkeley
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
# 1. Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Proxy between a python fuzzer and JQF. Communicates to 
# both via pipes -- those launched by the fuzzer and two whose names
# are given as command line args and must be created before 
# launching the proxy. 
#
# This proxy is general and can be used to exchange information
# from any external utility writing in the command-line pipes.
#
# author: Caroline Lemieux

import sys
import os
import tempfile
import logging


class JQFProgram:
    # test_class and test_method are the class and methods to test with JQF
    def __init__(self, test_class, test_method, jqf_dir):
        #TODO: maybe perform a pilot run to check you haven't messed up test_class and method and jqf_dir
        # Note: this must be consistent with the Java AFLGuidance COVERAGE_MAP_SIZE
        self.coverage_map_size =  1 << 16
        # set up temporaries: p2j and j2p FIFOs and shared input file
        self.temp_dir_name = tempfile.TemporaryDirectory()
        to_java_name = os.path.join(self.temp_dir_name, "p2j")
        from_java_name = os.path.join(self.temp_dir_name, "j2p")
        # If these throw exceptions we shouldn't go on
        os.mkfifo(to_java_name)
        os.mkfifo(from_java_name)
        self.p2j = open(to_java_name, 'wb')
        self.j2p = open(from_java_name, 'rb')
        self.cur_input_name = os.path.join(self.temp_dir_name, ".cur_input")
        # start up JQF
        script_name = os.path.join(jqf_dir, "scripts/jqf-driver.sh")        
        # not storing this anywhere makes sure this runs in the background
        subprocess.Popen(script_name, "edu.berkeley.cs.jqf.fuzz.afl.AFLDriver", test_class, test_method, self.cur_input_name, to_java_name, from_java_name)
        # but let's just be safe and make sure we're not stalled...
        logging.debug("[JQF.py LOG] Done initializing JQF on %s.%s!" % (test_class, test_method))        


    def run_on_input(input_str):
        # write the input to file for JQF
        with open(self.cur_input_name, "w") as input_file:
            input_file.write(input_str)
        # Tell JQF we're ready to run
        if (self.p2j.write(b"HELO") < 4):
            raise Exception("Failed to say hello to JAVA in run_on_input")
        # need to flush the pipe
        self.p2j.flush()
        logging.debug("[JQF.py LOG] Done initializing JQF on %s.%s!" % (test_class, test_method))        
        return_status = self.j2p.read(4)
        if len(return_status) < 4:
            raise Exception("Didn't get enough bytes in Java return status (only got %d)" % len(return_status))

        coverage_map = self.j2p.read(coverage_map_size)        
        if len(coverage_map_size) < coverage_map_size:
            raise Exception("Didn't get enough bytes in Java coverage map size (only got %d)" % len(coverage_map_size))
        #TODO: could use the coverage map to get coverage data, discarding it for now.

        if int.from_bytes(return_status) == 0:
            ret_status = "VALID"
        elif int.from_bytes(return_status) == (1 << 8):
            ret_status = "INVALID"
        elif int.from_bytes(return_status) == 9:
            ret_status = "TIMEOUT"
        elif int.from_bytes(return_status) == 6:
            ret_status = "FAILURE"
        else:
            raise Exception("Unexpected return status %d" % int.from_bytes(return_status))
        
        return ret_status
