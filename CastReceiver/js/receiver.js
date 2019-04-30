'use strict';

const context        = cast.framework.CastReceiverContext.getInstance()
const playerManager  = context.getPlayerManager()
const playbackConfig = new cast.framework.PlaybackConfig()

var refererHeader

const updateRefererHeader = loadRequestData => {
  if (loadRequestData.customData && loadRequestData.customData.referer) {
    refererHeader = loadRequestData.customData.referer
  }
  return loadRequestData
}

const updateNetworkRequestInfo = networkRequestInfo => {
  if (refererHeader && networkRequestInfo && networkRequestInfo.headers) {
    networkRequestInfo.headers.referer = refererHeader
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
