require 'pry-remote-em/server'

class Foo
  def initialize(x, y)
    binding.remote_pry_em
  end
end

puts EM.run { Foo.new 10, 20 } 
