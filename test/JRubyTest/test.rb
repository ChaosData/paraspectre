require 'java'

require 'socket'

s = Socket.tcp '127.0.0.1', 4444

#STDOUT.reopen(s)
#STDIN.reopen(s)
#STDERR.reopen(s)

$stdout = s
$stdin = s

#s.puts 's.puts'

puts "test"

