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
//    case ITCM_RAM_ADDR_WIDTH => 16 // 64KByte
    case ITCM_RAM_ADDR_WIDTH => 12 // 4KByte
    case ITCM_RAM_DEPTH => 1 << (here(ITCM_RAM_ADDR_WIDTH) - 2)
    // DTCM
//    case DTCM_RAM_ADDR_WIDTH => 16 // 64KByte
    case DTCM_RAM_ADDR_WIDTH => 12 // 4KByte
    case DTCM_RAM_DEPTH => 1 << (here(DTCM_RAM_ADDR_WIDTH) - 2)
  }
)


