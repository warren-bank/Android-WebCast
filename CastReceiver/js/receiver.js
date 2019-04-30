'use strict';

const context        = cast.framework.CastReceiverContext.getInstance()
const playerManager  = context.getPlayerManager()
const playbackConfig = new cast.framework.PlaybackConfig()

var refererHeader

const updateRefererHeader = loadRequestData => {
  if (loadRequestData.customData && loadRequestData.customData.referer) {
    refererHeader = loadRequestData.customData.referer
  }
  else if (loadRequestData.media && loadRequestData.media.customData && loadRequestData.media.customData.referer) {
    refererHeader = loadRequestData.media.customData.referer
  }
  return loadRequestData
}

const headers_remove = ["chrome-proxy","cast-device-capabilities"]
const headers_update = {
  "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36"
}

const updateNetworkRequestInfo = networkRequestInfo => {
  if (networkRequestInfo && networkRequestInfo.headers) {
    headers_remove.forEach(name => {
      delete networkRequestInfo.headers[name]
    })

    Object.assign(networkRequestInfo.headers, headers_update)

    if (refererHeader) {
      networkRequestInfo.headers['referer'] = refererHeader
    }
  }
  return networkRequestInfo
}

playerManager.setMessageInterceptor(
  cast.framework.messages.MessageType.LOAD,
  updateRefererHeader
)

playbackConfig.manifestRequestHandler = updateNetworkRequestInfo
playbackConfig.segmentRequestHandler  = updateNetworkRequestInfo

context.start({playbackConfig})
