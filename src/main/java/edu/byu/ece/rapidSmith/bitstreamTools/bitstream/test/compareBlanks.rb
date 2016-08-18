require 'fileutils'

PARTS = ["xc4vlx15", "xc4vlx25", "xc4vlx40", "xc4vlx60", "xc4vlx80", "xc4vlx100", "xc4vlx160", "xc4vlx200",
         "xc4vsx25", "xc4vsx35", "xc4vsx55", 
         "xc4vfx12", "xc4vfx20", "xc4vfx40", "xc4vfx60", "xc4vfx100", "xc4vfx140"]

PACKAGES = ["sf363", "ff668", "ff672", "ff676", "ff1148", "ff1152", "ff1513", "ff1517"]

PARTS_PACKAGES = 
 {"xc4vlx15" => [0, 1, 3],
  "xc4vlx25" => [0, 1, 3],
  "xc4vlx40" => [1, 4],
  "xc4vlx60" => [1, 4],
  "xc4vlx80" => [4],
  "xc4vlx100" => [4, 6],
  "xc4vlx160" => [4, 6],
  "xc4vlx200" => [6],

  "xc4vsx25" => [1],
  "xc4vsx35" => [1],
  "xc4vsx55" => [4],

  "xc4vfx12" => [0, 1],
  "xc4vfx20" => [2],
  "xc4vfx40" => [2, 5],
  "xc4vfx60" => [2, 5],
  "xc4vfx100" => [5, 7],
  "xc4vfx140" => [7]
}

PARTS.each do |part|
  PARTS_PACKAGES[part].each do |packageIndex|
    name = part
    name += PACKAGES[packageIndex]
    name += "-10"
    FileUtils.cd(name)

    puts `java edu.byu.ece.bitstreamTools.bitstream.test.ParserGenerator blank.bit blankJava.bit blankJava.mcs`

    `diff blank.bit blankJava.bit`
    puts name
    match = true;
    if $? != 0 then
      puts ".bit files don't match"
      match = false
    end
    `diff blank.mcs blankJava.mcs`
    if $? != 0 then
      puts ".mcs files don't match"
      match = false
    end
    if match then
      puts ".bit and .mcs files match"
    end
    FileUtils.cd("../")
  end
end
