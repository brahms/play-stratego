angular.module('app.stratego.piece', [])
    .factory('StrategoPiece', ['$log', 
(log) ->              
    imagesPromises = []

    images = {}

    for i in [1..12]
        rdefer = $.Deferred()
        bdefer = $.Deferred()
        bkey = "b#{i}"
        rkey = "r#{i}"
        bimage = new Image()
        bimage.onload = ->
            bdefer.resolve()
        rimage = new Image()
        rimage.onload = ->
            rdefer.resolve()

        bimage.src = "#{ASSETS}images/stratego/#{bkey}.png"
        rimage.src = "#{ASSETS}images/stratego/#{rkey}.png"

        images[rkey] = rimage
        images[bkey] = bimage

        imagesPromises.push rdefer.promise()
        imagesPromises.push bdefer.promise()

    class StrategoPiece
        constructor: (@boardVal) ->
            @image = new Kinetic.Image {
                image: images[@getImageKey()]
                draggable: true
            }
            if @boardVal > 100
                @pieceVal = @boardVal - 100
                @color = 'r'
            else 
                @color == 'b'
        getImageKey: => 
            "#{@color}#{@pieceVal}"
        toString: =>
            "StrategoPiece[@getImageKey()]"


    StrategoPiece.isLoadedPromise = $.when(imagesPromises)
    StrategoPiece.VAL_SPY = 1
    StrategoPiece.VAL_MINER = 3
    StrategoPiece.VAL_SCOUT = 2
    StrategoPiece.VAL_GENERAL = 10
    StrategoPiece.VAL_BOMB = 11
    StrategoPiece.VAL_FLAG = 12
    return StrategoPiece
])