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
                x: 100
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
            @rect.visible(false)
            @text.visible(false)

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
                            width: 450
                })

            @animationsEnabled = false
            @offsetx = 0
            #layers for the square overlays / pieces 
            @squaresLayer = new Kinetic.Layer()
            #layer for the sideboard overlays
            @sideboardLayer = new Kinetic.Layer()
            # layer for the background 
            @backgroundLayer = new Kinetic.Layer()
            #Layer for the turn arrows
            @turnLayer = new Kinetic.Layer()

            #layer to show a waint for player screen
            @pendingLayer = new Kinetic.Layer()

            @phase = null
            @state = 'PENDING'
            promises = []
            @bluePlayerReady = false
            @redPlayerReady = false

            # @stage.on 'click', () =>
            #     x = @stage.getPointerPosition().x #- bgImage.getPosition().x;
            #     y = @stage.getPointerPosition().y #- bgImage.getPosition().y;
            #     log.debug "#{x}, #{y}"

            promises.push @_createBackgroundImage()
            promises.push @_createSquares()
            promises.push @_createSideboards(@isRed)
            @_createPendingLayer()
            @stage.add(@backgroundLayer)
            @stage.add(@squaresLayer)
            @stage.add(@sideboardLayer)
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
            if @pendingLayer? then @pendingLayer.draw()
        setRunning: ->
            log.debug("#{@} setRunning")
            @state = 'RUNNING'
            @pendingText.stop()
            @pendingLayer.destroy()
            @pendingLayer = null
        placePiece: (piece, x, y) ->
            log.debug("#{@} placePiece(#{piece} #{x}, #{y})")
            @matrix[x][y].setPiece(piece)

        placePieceAndUpdateSideboard: (piece, x, y) ->
            log.debug("#{@} placePiece(#{piece} #{x}, #{y})")
            promise = @matrix[x][y].setPiece(piece)
            if piece.value < 13
                if (piece instanceof RedPiece) then @redSideboard.decrementSideboardCount(piece)
                else @blueSideboard.decrementSideboardCount(piece)
            promise
            
        setEmpty: (x, y) ->
            log.debug("#{@} setEmpty(#{x} #{y})")
            @matrix[x][y].setEmpty()
        setEmptyAndUpdateSideboard: (x, y) ->
            log.debug("#{@} setEmptyAndUpdateSideboard(#{x} #{y})")
            piece = @matrix[x][y].getPiece()
            @matrix[x][y].setEmpty()
            
            if (piece.value < 13)
                if piece instanceof RedPiece then @redSideboard.incrementSideboardCount(piece)
                else @blueSideboard.incrementSideboardCount(piece)

        movePiece: (x, y, newX, newY) ->
            piece = @matrix[x][y].getPiece()
            log.debug("#{@} movePiece(#{piece} #{x}, #{y} -> #{newX}, #{newY})")
            @matrix[x][y].setEmpty()
            @matrix[newX][newY].setPiece(piece)
        getPiece: (x, y) ->
            if !x? then throw "getPiece null x"
            if !y? then throw "getPiece null y"
            square = @matrix[x][y]
            if square == Boundary then throw "Empty Piece"

            square.getPiece()
        incrementSideboardCount: (piece) ->
            log.debug("incrementSideboardCount for: #{piece}")
            if piece instanceof RedPiece
                @redSideboard.incrementSideboardCount(piece.value)
            else if piece instanceof BluePiece
                @blueSideboard.incrementSideboardCount(piece.value)
            else
                throw "Invalid piece: #{piece}"
        enableAnimations: () ->
            log.debug("#{@} enableAnimations")
            @animationsEnabled = true
        disableAnimations: () ->
            log.debug("#{@} disableAnimations")
            @animationsEnabled = false

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
        commitThisPlayer: ->
            log.debug("commitThisPlayer")
            if @isRed then @redPlayerReady = true
            else @bluePlayerReady = true

            if @bluePlayerReady and @redPlayerReady then @startRunningPhase()
            log.debug("#{@} commitThisPlayer phase: #{@phase}")
        getDistance: (x, y, newX, newY) ->
            dist = null
            if (x== newX) then  dist = Math.abs(newY - y)
            else dist = Math.abs(newX - x)
            dist
        isDiagonal: (x, y, newX, newY) ->
            x != newX and y != newY
        isThroughLakes: (x, y, newX, newY) ->
            byX = if (x < newX) then 1 else -1
            byY = if (y < newY) then 1 else -1
            if (x == newX)
                for checkY in [newY..y] by byY
                    if @matrix[x][checkY] == Boundary then return true
            else
                for checkX in [newX..x] by byX
                    if @matrix[checkX][y] == Boundary then return true 
            false

        commitOtherPlayer: ->
            log.debug("commitOtherPlayer")
            if @isRed then @bluePlayerReady = true
            else @redPlayerReady = true

            if @bluePlayerReady and @redPlayerReady then @startRunningPhase()
            log.debug("#{@} commitOtherPlayer phase: #{@phase}")
        startRunningPhase: ->
            @phase = "RUNNING"
            @redSideboard.initForRunningPhase()
            @blueSideboard.initForRunningPhase()
        setPhase: (phase) ->
            log.debug("#{@} setPhase #{phase}")
            @phase = phase
        setState: (state) ->
            log.debug("#{@} setState #{state}")
        enableSideboard: ->
            log.debug("#{@} enableSideboard")
            if (@isRed) then @redSideboard.enableDragging() 
            else @blueSideboard.enableDragging()
        killPiece: (attackAction) ->
            {x, y, newX, newY, result, user, attacker, defender} = attackAction
            log.debug("#{@} killPiece #{attackAction}")
            switch attackAction.result
                when "DEFENDER_DIES"
                    @matrix[newX][newY].getPiece().kill()
                    @matrix[newX][newY].setEmpty()
                    @movePiece(x, y, newX, newY)
                    @incrementSideboardCount(defender)
                when "ATTACKER_DIES"
                    @matrix[x][y].getPiece().kill()
                    @matrix[x][y].setEmpty()
                    @incrementSideboardCount(attacker)
                when "BOTH_DIE"
                    @matrix[x][y].getPiece().kill()
                    @matrix[newX][newY].getPiece().kill()
                    @matrix[newX][newY].setEmpty()
                    @matrix[x][y].setEmpty()
                    @incrementSideboardCount(attacker)
                    @incrementSideboardCount(defender)
                else
                    throw "Kill piece unknown result: " + result
            

        animatedKill: (attackAction) ->
            {x, y, newX, newY, result, user, attacker, defender} = attackAction

            defer = Q.defer()
            @killPiece(attackAction)
            @hidePiece(x, y)
            @hidePiece(newX, newY)
            @draw()
            attackerImage = @getSideboardImage(attacker)
            defenderImage = @getSideboardImage(defender)

            attackSquare = @matrix[x][y]
            defendSquare = @matrix[newX][newY]

            attackX = attackSquare.layerX
            attackY = attackSquare.layerY
            defendX = defendSquare.layerX
            defendY = defendSquare.layerY

            resetAttackX = attackerImage.x()
            resetAttackY = attackerImage.y()
            resetDefendX = defenderImage.x()
            resetDefendY = defenderImage.y()

            attackerImage.x(attackX)
            attackerImage.y(attackY)
            defenderImage.x(defendX)
            defenderImage.y(defendY)


            anim1Defer = Q.defer()
            anim1 = new Kinetic.Tween {
                node: attackerImage
                duration: 3
                x: defendX
                y: defendY
                onFinish: -> log.debug("anim1 finish"); anim1Defer.resolve()
            }
            anim1.play()
            anim1Defer.promise
                .then ->
                    log.debug('anim 1 done')
                    attackDeathAnimDefer = Q.defer()
                    attackDeathAnim = new Kinetic.Tween {
                        node: attackerImage
                        x: resetAttackX
                        y: resetAttackY
                        duration: 3
                        onFinish: -> attackDeathAnimDefer.resolve()
                    }
                    defendDeathAnimDefer = Q.defer()
                    defendDeathAnim = new Kinetic.Tween {
                        node: defenderImage
                        x: resetDefendX
                        y: resetDefendY
                        duration: 3
                        onFinish: -> defendDeathAnimDefer.resolve()
                    }
                    switch result
                        when "ATTACKER_DIES"
                            attackDeathAnim.play()
                            attackDeathAnimDefer.promise
                        when "DEFENDER_DIES"
                            defendDeathAnim.play()
                            defendDeathAnimDefer.promise
                        when "BOTH_DIE"
                            attackDeathAnim.play()
                            defendDeathAnim.play()
                            Q.all([attackDeathAnimDefer.promise, defendDeathAnimDefer.promise])
                        else
                            throw "Kill piece unknown result: #{result}"
                .then () =>
                    log.debug('anim2 done')
                    attackerImage.x(resetAttackX)
                    attackerImage.y(resetAttackY)
                    defenderImage.x(resetDefendX)
                    defenderImage.y(resetDefendY)
                    @showPiece(x, y)
                    @showPiece(newX, newY)
                    @draw()
                    defer.resolve()

            defer.promise
        getSideboardImage: (piece) ->
            if !piece? then throw "getSideboardImage null piece"
            if piece.value == 13 then throw "getSideboardImage bad piece"

            if piece instanceof RedPiece then return @redSideboard.getImage(piece.value)
            else @blueSideboard.getImage(piece.value)
        getSideboardCount: (piece) ->
            if !piece? then throw "getSideboardCount null piece"
            if piece.value == 13 then throw "getSideboardCount bad piece"

            if piece instanceof RedPiece then return @redSideboard.getCount(piece.value)
            else @blueSideboard.getCount(piece.value)


        animatedMove: (x, y, newX, newY) ->
            defer = Q.defer()
            piece = @matrix[x][y].getPiece()
            log.debug "#{@} animatedMove #{piece}: #{x}, #{y} -> #{newX}, #{newY}"
            newSquare = @matrix[newX][newY]
            velocity = 50
            xChange = newX - x
            yChange = newY - y
            anim = new Kinetic.Tween {
                node: piece.image
                x: newSquare.layerX
                y: newSquare.layerY
                onFinish: =>
                    @matrix[x][y].setEmpty()
                    @matrix[newX][newY].setPiece(piece)
                    log.debug('move anim finished')
                    defer.resolve()
                duration: 2
            }
            anim.play()

            defer.promise

        enableDragging: ->
            log.debug("#{@} enableDragging")
            for x in [1..10]
                for y in [1..10]
                    square =  @matrix[x][y]
                    if square instanceof StrategoSquare and @isRed and square.hasRedPiece() 
                        square.draggableOn()
                    else if square instanceof StrategoSquare and !@isRed and square.hasBluePiece() 
                        square.draggableOn()
        disableDragging: ->
            for x in [1..10]
                for y in [1..10]
                    square = @matrix[x][y]
                    if square then square.draggableOff()
        showPiece: (x, y) ->
            piece = @getPiece(x, y)
            if piece != StrategoPiece.Empty then piece.show()
        hidePiece: (x, y) ->
            piece = @getPiece(x, y)
            if piece != StrategoPiece.Empty then piece.hide()

        isOutOfBounds: (x, y) ->
            (x < 1 or x > 10) or (y < 1 or y > 10)
        isOnUsersSide: (x, y) ->
            (@isRed and y <= 4) or (!@isRed and y >= 7)

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
                @backgroundLayer.add(new Kinetic.Rect({
                    x: 0
                    y: 0
                    width: @stage.width()
                    height: @stage.height()
                    fill: false
                    stroke: 'black'
                    strokeWidth: 5
                }))
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