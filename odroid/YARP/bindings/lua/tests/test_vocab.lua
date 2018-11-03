#!/usr/bin/lua

-- Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
-- Copyright (C) 2018 Juan G. Victores
-- All rights reserved.
--
-- This software may be modified and distributed under the terms of the
-- BSD-3-Clause license. See the accompanying LICENSE file for details.

require("yarp")

function test_vocab()
  vocab = yarp.encode("abcd")
  assert("number" == type(vocab))
  assert(1684234849 == vocab)
  assert("abcd" == yarp.decode(vocab))
  vocab2 = yarp.createVocab('a','b','c','d')
  assert("number" == type(vocab2))
  assert (vocab == vocab2)
end

function test_vocab_pixel_types_enum()
  assert("number" == type(yarp.VOCAB_PIXEL_RGB))
  assert(6449010 == yarp.VOCAB_PIXEL_RGB) -- VOCAB3
  assert(761423730 == yarp.VOCAB_PIXEL_RGB_SIGNED) -- VOCAB4 with '-' 
  assert(909209453 == yarp.VOCAB_PIXEL_MONO16) -- VOCAB4 with '1' and '6'
end

function test_vocab_global_scope()
  assert("number" == type(yarp.VOCAB_CM_POSITION))
  assert(7565168 == yarp.VOCAB_CM_POSITION) -- VOCAB3
  assert(1685286768 == yarp.VOCAB_CM_POSITION_DIRECT) -- VOCAB4
  assert(6514534 == yarp.VOCAB_FRAMEGRABBER_CONTROL) -- VOCAB4
end

test_vocab()
test_vocab_pixel_types_enum()
test_vocab_global_scope()

