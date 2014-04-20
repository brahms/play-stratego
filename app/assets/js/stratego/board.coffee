angular.module('app.stratego.board', ['app.stratego.square', 
    'app.stratego.pieces', 
    'app.stratego.functions',
    'app.stratego.sideboards'])
.factory('StrategoBoard', ['$log', '$q', '$window', 
    'StrategoSquare', 
    'StrategoFunctions', 
    'StrategoPieces',
    'StrategoSideboards'
(log, Q, $window, StrategoSquare, StrategoFunctions, StrategoPieces, StrategoSideboards) ->
    Boundary = -1
    Empty = 0
    {StrategoPiece, BluePiece, RedPiece} = StrategoPieces
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions
    {RedSideboard, BlueSideboard} = StrategoSideboards

    BLUE_NAME_Y = 30
    RED_NAME_Y = 580

    RED_NAME_X = BLUE_NAME_X = 550
    NAME_FONT_SIZE = 20

    class PendingText 
        constructor: ({stage, layer}) ->
            @ticks = 1
            @rect = new Kinetic.Rect {
                height: stage.getHeight()
                width: stage.getWidth()
                x: 0
                y: 0
                fill: 'grey',
                stroke: 'black',
                strokeWidth: 4,
                opacity: 0.3
            }
            @text = new Kinetic.Text {
                x: 250
                y: 250
                text: @_createText()
                fontFamily: 'Calibri'
                fill: 'red'
                stroke: 'black'
                fontSize: 40
            }
            layer.add(@rect)
            layer.add(@text)
            @interval = null
        _createText: ->
            "Loading" + Array(@ticks).join('.')
        _interval: =>
            @text.text(@_createText())
            @text.getLayer().draw()
            @ticks += 1
            if @ticks > 10 then @ticks = 1
        start: ->
            @interval = $window.setInterval(@_interval, 1 * 1000)
        stop: ->
            if @interval then $window.clearInterval(@interval)


    class TurnState
        constructor: (layer) ->
            if !layer? then throw "TurnState layer null"
            width = 100
            height = 100
            startX = BLUE_NAME_X
            startY = Math.floor((RED_NAME_Y-BLUE_NAME_Y)/2)
            @redsTurn = false
            @redArrow = new Kinetic.Rect {
                x: startX
                y: startY
                sides: 4
                height: 100
                width: 100
                fill: 'red'
                stroke: 'black'
                strokeWidth: 10
                visible: false

            }
            @blueArrow = new Kinetic.Rect {
                x: startX
                y: startY
                sides: 4
                height: 100
                width: 100
                fill: 'blue'
                stroke: 'black'
                strokeWidth: 10
                visible: false

            }
            layer.add(@redArrow)
            layer.add(@blueArrow)
            log.debug("#{@} constructor")
        setRedTurn: ->
            if not @redsTurn
                @toggle()

        setBlueTurn: ->
            if @redsTurn
              @toggle()  
        toggle: ->
            if (@redsTurn) 
                @blueArrow.visible(true)
                @redArrow.visible(false)
            else
                @redArrow.visible(true)
                @blueArrow.visible(false)
            @redsTurn = !@redsTurn

        toString: ->
            "TurnState[Turn: #{if @isRed then "RedPlayer" else "BluePlayer"}]"

    class StrategoBoard extends EventEmitter
        constructor: ({canvas, isRed}) ->
            @canvas  = canvas
            @isRed = isRed
            @matrix = [
                [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1] #x=0
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=1
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=2
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=3
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=4
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=5
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=6
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=7
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=8
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=9
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=10
                [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1] #x=11
            ]   #y0 y1 y2 y3 y4 y5 y6 y7 y8 y9 y10 y11
            @isRedTurn = null

        init: ->
            log.debug "Creating board at element: #{@canvas}"
            @stage = new Kinetic.Stage({
                            container: @canvas
                            height: 600
                            width: 700
                })
            @offsetx = (@stage.getWidth() / 2) - (525/2)
            #layers for the square overlays / pieces 
            @squaresLayer = new Kinetic.Layer()
            #layer for the sideboard overlays
            @sideboardLayer = new Kinetic.Layer()
            # layer for the background 
            @backgroundLayer = new Kinetic.Layer()

            #layer for the uesrnames
            @userLayer = new Kinetic.Layer()

            #Layer for the turn arrows
            @turnLayer = new Kinetic.Layer()

            #layer to show a waint for player screen
            @pendingLayer = new Kinetic.Layer()

            @phase = null
            @state = 'PENDING'
            promises = []

            # @stage.on 'click', () =>
            #     x = @stage.getPointerPosition().x #- bgImage.getPosition().x;
            #     y = @stage.getPointerPosition().y #- bgImage.getPosition().y;
            #     log.debug "#{x}, #{y}"

            promises.push @_createBackgroundImage()
            promises.push @_createSquares()
            promises.push @_createSideboards(@isRed)
            @_createTurnState()
            @_createPendingLayer()
            @stage.add(@backgroundLayer)
            @stage.add(@squaresLayer)
            @stage.add(@sideboardLayer)
            @stage.add(@userLayer)
            @stage.add(@turnLayer)
            @stage.add(@pendingLayer)


            @promise = Q.all(promises)
            @promise.then () =>
                log.debug("#{@} done loading")
            return @promise
        draw: ->
            log.debug("#{@} draw")
            @backgroundLayer.draw()
            @sideboardLayer.draw()
            @squaresLayer.draw()
            @userLayer.draw()
            @turnLayer.draw()
            if @pendingLayer? then @pendingLayer.draw()

        placePiece: (piece, x, y) ->
            log.debug("#{@} placePiece(#{piece} #{x}, #{y})")
            @matrix[x][y].setPiece(piece)
        killPiece: (piece, x, y) -> 
            log.debug("#{@} killPiece(#{piece} #{x}, #{y})")
        movePiece: (piece, x, y) ->
            log.debug("#{@} movePiece(#{piece} #{x}, #{y})")
        attackPiece: (attacker, defender) ->
        decrementSideboardCount: (piece) ->
            if piece instanceof RedPiece then @redSideboard.decrementSideboardCount(piece)
            else @blueSideboard.decrementSideboardCount(piece)
        getSquareForLayerPoint: ({layerX, layerY}) ->
            for x in [1..10]
                for y in [1..10]
                    square = @matrix[x][y]
                    if (square instanceof StrategoSquare and square.intersects(layerX,layerY))
                        return square
            return null
        setRedPlayer: (player) ->
            text = new Kinetic.Text {
                x: RED_NAME_X,
                y: RED_NAME_Y,
                text: player.username,
                fontSize: NAME_FONT_SIZE,
                fontFamily: 'Calibri',
                fill: 'black'
            }
            log.debug("Setting red player to #{player.username}")
            @userLayer.add(text)
        setPhase: (phase) ->
            log.debug("#{@} setPhase #{phase}")
            @phase = phase
        setState: (state) ->
            log.debug("#{@} setState #{state}")

        setBluePlayer: (player) ->
            text = new Kinetic.Text {
                x: BLUE_NAME_X,
                y: BLUE_NAME_Y,
                text: player.username,
                fontSize: NAME_FONT_SIZE,
                fontFamily: 'Calibri',
                fill: 'black'
            }
            log.debug("Setting blue player to #{player.username}")
            @userLayer.add(text)
        enableSideboard: ->
            log.debug("#{@} enableSideboard")
            if (@isRed) then @redSideboard.enableDragging() 
            else @blueSideboard.enableDragging()
        enableDragging: ->
            log.debug("#{@} enableDragging")
            for x in [1..10]
                for y in [1..10]
                    square =  @matrix[x][y]
                    if square and @isRed and square.hasRedPiece() then square.draggableOn()
                    else if square and !isRed and square.hasBluePiece() then square.draggableOn()
        disableDragging: ->
            for x in [1..10]
                for y in [1..10]
                    square = @matrix[x][y]
                    if square then square.draggableOff()

        ### Creates the sideboards ###
        _createSideboards: ->
            @redSideboard = new RedSideboard({
                layer: @sideboardLayer
                emitter: @
            })
            @blueSideboard = new BlueSideboard({
                layer: @sideboardLayer
                emitter: @
            })
            
            Q.all([@redSideboard.init(), @blueSideboard.init()])

        _createBackgroundImage: ->
            defer = Q.defer()

            bgImageObject = new Image()
            bgImageObject.src = "#{ASSETS}images/stratego/board.jpg"
            bgImageObject.onload = () =>
                log.debug('boardImage loaded') 
                
                bgImage = new Kinetic.Image {
                    x: @offsetx
                    y: 0
                    image: bgImageObject
                    width: 450
                    height: 600
                    visible: true
                }

                @backgroundLayer.add(bgImage)
                defer.resolve()

            return defer.promise

        _onPiecePlaced: (event) =>
            log.debug("#{@} _onPiecePlaced: #{event}")
            @emit('piece')
        _createTurnState: ->
            @turnState = new TurnState(@turnLayer)

        _createPendingLayer: ->
            @pendingText = new PendingText(layer: @pendingLayer, stage: @stage)
            @pendingText.start()

        _createSquares: ->
            defer = Q.defer()
            defer.resolve()
            for x in [1..10]
                for y in [1..10]
                    if (@matrix[x][y] == Empty)
                        @matrix[x][y] = new StrategoSquare({
                            x: x 
                            y: y
                            layer: @squaresLayer
                            boardOffset: @offsetx
                            emitter: @
                        })

            return defer.promise
        toString: ->
            "StrategoBoard[]"


])