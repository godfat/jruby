require File.expand_path('../shared/include', __FILE__)
require 'set'

describe "SortedSet#member?" do
  it_behaves_like :sorted_set_include, :member?
end
