# encoding: utf-8

require 'spec_helper'
require 'support/install_common'

describe 'kafka::default' do
  it_behaves_like 'an install method'
end
