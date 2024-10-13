this = @this
constructor = @constructor
method = @method
args = @args
pkg = @pkg
eval_res = eval @eval

secret = SecureRandom.random_bytes(16).unpack('H*')[0]
port = 1024 + @uid + @tid

secret_bytes = secret.to_java_bytes()
port_bytes = [port].pack('I>').to_java_bytes()

@unixPing.invoke(nil, secret_bytes, port_bytes)

begin
  binding.remote_pry('127.0.0.1', port, {:secret => secret})
rescue => e
  puts e
end
