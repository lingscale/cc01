package lingscale.cc01.core

// import chisel3.Module
import chisel3.Bundle
import lingscale.cc01.config.{Parameters, Config}

class CoreConfig extends Config((site, here, up) => {
    // Core
    case XLEN => 32
    // IcbIO
    case IcbKey => new IcbParameters(
      addrBits = here(XLEN),
      dataBits = 32)
    // ITCM
    case ITCM_RAM_ADDR_WIDTH => 16 // 64KByte 16k*32bits
//    case ITCM_RAM_ADDR_WIDTH => 10 // 1KByte
    case ITCM_RAM_DEPTH => 1 << (here(ITCM_RAM_ADDR_WIDTH))
    // DTCM
    case DTCM_RAM_ADDR_WIDTH => 16 // 64KByte 16k*32bits
//    case DTCM_RAM_ADDR_WIDTH => 10 // 1KByte
    case DTCM_RAM_DEPTH => 1 << (here(DTCM_RAM_ADDR_WIDTH))
  }
)


