angular.module('app.stratego.sideboards', ['app.stratego.actions', 
    'app.stratego.functions',
    'app.stratego.pieces'])
    .factory('StrategoSideboards', ['$log', '$q', 'StrategoFunctions',  'StrategoPieces', 'StrategoActions'
(log, Q, StrategoFunctions, StrategoPieces, StrategoActions) ->
    
    {StrategoPiece, BluePiece, RedPiece} = StrategoPieces
    {PlacePieceAction} = StrategoActions
    START_X = 10
    END_X  = 450
    RED_Y = 560
    BLUE_Y = 10
    TEXT_OFFSET_X = 20
    TEXT_OFFSET_Y = 20            
    SQUARE_WIDTH = Math.floor((END_X-START_X)/12)
    SQUARE_HEIGHT = SQUARE_WIDTH

    getCenter =  (image) -> {
        x: (image.x() + (image.width()/2))
        y: (image.y() + (image.height()/2))
    }
    class SideboardSquare
        constructor: ({color, value, count, layer, emitter}) ->
            defer = Q.defer()
            @value = value
            @count = count
            @color = color
            @layer = layer
            @emitter = emitter
            @promise = defer.promise
            @draggable = false
            if !color? then throw "SideboardSquare Null color"
            if !value? then throw "SideboardSquare Null value"
            if !count? then throw "SideboardSquare Null count"
            if !layer? then throw "SideboardSquare Null layer"
            if !emitter? then throw "SideboardSquare Null emitter"

            [startX, startY] = if (@color=='r') then [START_X, RED_Y] else [START_X, BLUE_Y]
   
            x = startX + SQUARE_WIDTH*(value-1)
            y = startY
            @startX = x
            @startY = y
            imgElement = new Image()
            imgElement.src = @_getImageUrl()
            imgElement.onload = () =>
                log.debug("#{@} loaded")
                @countText = new Kinetic.Text {
                    x: x+TEXT_OFFSET_X,
                    y: y+TEXT_OFFSET_Y,
                    text: @count,
                    fontSize: 15,
                    fontFamily: 'Calibri',
                    fill: 'yellow'
                }
                @draggableImage = new Kinetic.Image {
                    image: imgElement
                    draggable: false
                    x: x
                    y: y
                    width: SQUARE_WIDTH
                    height: SQUARE_HEIGHT
                }

                @staticImage = new Kinetic.Image {
                    image: imgElement
                    draggable: false
                    x: x
                    y: y
                    width: SQUARE_WIDTH
                    height: SQUARE_HEIGHT
                }
                layer.add(@staticImage)
                layer.add(@draggableImage)
                layer.add(@countText)
                @draggableImage.on('dragend', =>
                    log.debug("#{@} placed x: #{@draggableImage.x()} y: #{@draggableImage.y()}")
                    @emitter.emit('place', @, getCenter(@draggableImage))
                )
                defer.resolve()

        reset: () =>
            log.debug("Reset: #{@}")
            @draggableImage.x(@startX)
            @draggableImage.y(@startY)
            @draggableImage.getLayer().draw()
        decrementCount: () =>
            if (@count > 0) 
                @count = @count - 1
                @countText.text(@count)
                if @count == 0 then @draggableImage.draggable(false)
                @countText.getLayer().draw()
            else
                log.warn("Cann't decerment count from #{@count}")
        incrementCount: () =>
            @count = @count + 1
            @countText.text(@count)
            if @draggable then @draggableImage.draggable(true)
        draggableOn: =>
            if @count > 0  
                log.debug("#{@} enabling dragging")
                @draggableImage.draggable(true)
            @draggable = true
        draggableOff: =>
            @draggableImage.draggable(false)
            @draggable = false
        getDraggedX: ->
            @draggableImage.x()
        getDraggedY: ->
            @draggableImage.y()
        setCountZero: ->
            @count = 0
            @draggableOff()
            @countText.text(0)
        getImage: ->
            @draggableImage

        toString: =>
            "SideboardSquare[#{@color}#{@value} count: #{@count}]"


        _getImageUrl: () ->
            "#{ASSETS}images/stratego/#{@color}#{@value}.png"





    class StrategoSideboard
        constructor: ({color, layer, counts, emitter}) ->
            @color = color
            @squares = {}
            @layer = layer
            @emitter = emitter
            if !color? then throw "StrategoSideboard Null color"
            if !layer? then throw "StrategoSideboard Null layer"
            if !emitter? then throw "StrategoSideboard Null emitter"
            if !emitter.emit? then throw "StrategoSideboard Null emitter.emit"
        incrementSideboardCount: (piece) ->
            value = if piece instanceof StrategoPiece then piece.value else piece
            @squares[value].incrementCount()
        decrementSideboardCount: (piece) ->
            value = if piece instanceof StrategoPiece then piece.value else piece
            @squares[value].decrementCount()
        init: ->
            log.debug("Init: #{@}")
            promises = []
            [StrategoPiece.MINVAL..StrategoPiece.MAXVAL].forEach (value) =>
                square = new SideboardSquare({
                        color: @color
                        value: value
                        count: counts?[value-1] or @getInitialCount(value)
                        layer: @layer
                        emitter: @emitter
                    })
                @squares[value] = square
                promises.push(square.promise)

            Q.all(promises).then () =>
                log.debug("#{@} Done init")
        getInitialCount: (value) ->
            switch (value)
              when StrategoPiece.BOMB then 6
              when StrategoPiece.FLAG then 1
              when StrategoPiece.SPY then 1
              when StrategoPiece.SCOUT then 8
              when StrategoPiece.MINER then 5
              when StrategoPiece.SERGENT then 4
              when StrategoPiece.LIEUTENANT then 4
              when StrategoPiece.CAPTAIN then 4
              when StrategoPiece.MAJOR then 3
              when StrategoPiece.COLONEL then 2
              when StrategoPiece.GENERAL then 1
              when StrategoPiece.MARSHAL then 1
        enableDragging: ->
            log.debug "#{@} enableDragging"
            for val, square of @squares
                square.draggableOn()
                log.debug("Enabled #{val} for dragging")
        disableDragging: ->
            log.debug "#{@} disableDragging"
            for val, square of @squares
                square.draggableOff()
                log.debug("Disabled #{val} for dragging")
        getCount: (value) ->
            @squares[value].count
        initForRunningPhase: ->
            for k, square of @squares
                square.setCountZero()
        getImage: (value) ->
            @squares[value].getImage()


    class RedSideboard extends StrategoSideboard
        constructor: (opts) ->
            opts.color = 'r'
            super(opts)
        getStartPos: ->
            {
                x: START_X,
                y: RED_Y
            }
        toString: ->
            "RedSideboard[]"


    class BlueSideboard extends StrategoSideboard
        constructor: (opts) ->
            opts.color = 'b'
            super(opts)
        getStartPos: ->
            {
                x: START_X,
                y: BLUE_Y
            }
        toString: ->
            "BlueSideboard[]"

            

    {
        StrategoSideboard: StrategoSideboard
        RedSideboard: RedSideboard
        BlueSideboard: BlueSideboard
    }

])