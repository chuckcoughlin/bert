// Generated from SpeechSyntax.g4 by ANTLR 4.7.2
package chuckcoughlin.bert.speech.antlr

import org.antlr.v4.runtime.Lexer

open class SpeechSyntaxLexer(input: CharStream?) : Lexer(input) {
    @get:Deprecated("")
    val tokenNames: Array<String?>
        get() = Companion.tokenNames
    val vocabulary: Vocabulary
        get() = VOCABULARY
    val grammarFileName: String
        get() = "SpeechSyntax.g4"
    val ruleNames: Array<String>
        get() = Companion.ruleNames
    val channelNames: Array<String>
        get() = Companion.channelNames
    val modeNames: Array<String>
        get() = Companion.modeNames
    val aTN: ATN
        get() = _ATN

    init {
        _interp = LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache)
    }

    companion object {
        init {
            RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION)
        }

        protected val _decisionToDFA: Array<DFA?>
        protected val _sharedContextCache: PredictionContextCache = PredictionContextCache()
        const val Freeze = 1
        const val Relax = 2
        const val Why = 3
        const val Article = 4
        const val Adjective = 5
        const val Adverb = 6
        const val Appendage = 7
        const val Are = 8
        const val As = 9
        const val Attribute = 10
        const val Axis = 11
        const val Be = 12
        const val Configuration = 13
        const val Controller = 14
        const val Do = 15
        const val Goals = 16
        const val Greeting = 17
        const val Have = 18
        const val Hold = 19
        const val How = 20
        const val Initialize = 21
        const val Isay = 22
        const val Is = 23
        const val It = 24
        const val List = 25
        const val Limb = 26
        const val Limits = 27
        const val Means = 28
        const val Metric = 29
        const val Mittens = 30
        const val Motors = 31
        const val Motor = 32
        const val Move = 33
        const val Of = 34
        const val Off = 35
        const val On = 36
        const val Joint = 37
        const val Pose = 38
        const val Properties = 39
        const val Property = 40
        const val Reset = 41
        const val Salutation = 42
        const val Save = 43
        const val Set = 44
        const val Side = 45
        const val Straighten = 46
        const val Take = 47
        const val Then = 48
        const val To = 49
        const val Unit = 50
        const val Value = 51
        const val You = 52
        const val What = 53
        const val When = 54
        const val Where = 55
        const val COMMA = 56
        const val COLON = 57
        const val DECIMAL = 58
        const val INTEGER = 59
        const val NAME = 60
        const val EQUAL = 61
        const val SLASH = 62
        const val PCLOSE = 63
        const val POPEN = 64
        const val DBLQUOTE = 65
        const val SNGLQUOTE = 66
        var channelNames = arrayOf(
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
        )
        var modeNames = arrayOf(
            "DEFAULT_MODE"
        )

        private fun makeRuleNames(): Array<String> {
            return arrayOf(
                "Freeze", "Relax", "Why", "Article", "Adjective", "Adverb", "Appendage",
                "Are", "As", "Attribute", "Axis", "Be", "Configuration", "Controller",
                "Do", "Goals", "Greeting", "Have", "Hold", "How", "Initialize", "Isay",
                "Is", "It", "List", "Limb", "Limits", "Means", "Metric", "Mittens", "Motors",
                "Motor", "Move", "Of", "Off", "On", "Joint", "Pose", "Properties", "Property",
                "Reset", "Salutation", "Save", "Set", "Side", "Straighten", "Take", "Then",
                "To", "Unit", "Value", "You", "What", "When", "Where", "COMMA", "COLON",
                "DECIMAL", "INTEGER", "NAME", "ALPHA", "DIGIT", "DASH", "EQUAL", "PERIOD",
                "SLASH", "UNDERSCORE", "PCLOSE", "POPEN", "DBLQUOTE", "SNGLQUOTE"
            )
        }

        val ruleNames = makeRuleNames()
        private fun makeLiteralNames(): Array<String?> {
            return arrayOf(
                null, null, null, "'why'", null, "'current'", null, null, "'are'", "'as'",
                null, null, null, "'configuration'", null, "'do'", null, null, null,
                "'hold'", "'how'", "'initialize'", "'i say'", "'is'", "'it'", null, null,
                "'limits'", "'means'", null, "'mittens'", null, null, null, null, "'off'",
                "'on'", null, "'pose'", null, null, "'reset'", null, "'save'", "'set'",
                null, "'straighten'", null, "'then'", "'to'", "'degrees'", null, "'you'",
                "'what'", "'when'", "'where'", "','", "':'", null, null, null, "'='",
                "'/'", "')'", "'('", "'\"'", "'''"
            )
        }

        private val _LITERAL_NAMES = makeLiteralNames()
        private fun makeSymbolicNames(): Array<String?> {
            return arrayOf(
                null, "Freeze", "Relax", "Why", "Article", "Adjective", "Adverb", "Appendage",
                "Are", "As", "Attribute", "Axis", "Be", "Configuration", "Controller",
                "Do", "Goals", "Greeting", "Have", "Hold", "How", "Initialize", "Isay",
                "Is", "It", "List", "Limb", "Limits", "Means", "Metric", "Mittens", "Motors",
                "Motor", "Move", "Of", "Off", "On", "Joint", "Pose", "Properties", "Property",
                "Reset", "Salutation", "Save", "Set", "Side", "Straighten", "Take", "Then",
                "To", "Unit", "Value", "You", "What", "When", "Where", "COMMA", "COLON",
                "DECIMAL", "INTEGER", "NAME", "EQUAL", "SLASH", "PCLOSE", "POPEN", "DBLQUOTE",
                "SNGLQUOTE"
            )
        }

        private val _SYMBOLIC_NAMES = makeSymbolicNames()
        val VOCABULARY: Vocabulary = VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES)

        @Deprecated("Use {@link #VOCABULARY} instead.")
        val tokenNames: Array<String?>

        init {
            tokenNames = arrayOfNulls(_SYMBOLIC_NAMES.size)
            for (i in tokenNames.indices) {
                tokenNames[i] = VOCABULARY.getLiteralName(i)
                if (tokenNames[i] == null) {
                    tokenNames[i] = VOCABULARY.getSymbolicName(i)
                }
                if (tokenNames[i] == null) {
                    tokenNames[i] = "<INVALID>"
                }
            }
        }

        val serializedATN = """悋Ꜫ脳맭䅼㯧瞆奤Dғ										
	
		
	
																		 	 !	!"	"#	#$	$%	%&	&'	'(	()	)*	*+	+,	,-	-.	./	/0	01	12	23	34	45	56	67	78	89	9:	:;	;<	<=	=>	>?	?@	@A	AB	BC	CD	DE	EF	FG	GH	H²
Æ
Þ
ļ
š
				


ű
Ƈ









Ƒ

ƫ
Ƽ
ǉ
Ǔ
Ȉ
ș
ɐ
                    ɭ
 !!!!!!!!!!!!!!!!!ɿ
!"""""""""""""""ʏ
"######ʖ
#$$$$%%%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&˛
&'''''(((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((ͤ
()))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))Ϫ
)******++++++++++++++++++Ѓ
+,,,,,----...............М
.///////////00000000000г
01111122233333333444ч
45555666667777788888899::;;Ѣ
;;;ѥ
;;;Ѩ;;;;Ѭ
;;;ѯ;<<Ѳ
<<<ѵ
<
<<Ѷ==Ѻ
=
==ѻ>>??@@AABBCCDDEEFFGGHHI	
	

!#%')+-/13579;= ?!A"C#E${"$"}G%I&K'M(O)Q*S+U,W-Y.[/]0_1a2c3e4g5i6k7m8o9q:s;u<w=y>{}?@ABCDC\c|2;Ӽ	
!#%')+-/13579;=?ACEGIKMOQSUWY[]_acegikmoqsuwy±ÅÇ	Ýß
ĻŠŢŦŰƆƐƒƪƬ!ƻ#ǈ%ǒ'ǔ)Ǚ+ǝ-Ǩ/Ǯ1Ǳ3ȇ5Ș7Ț9ȡ;ɏ=ɑ?ɬAɾCʎEʕGʗIʛK˚M˜OͣQϩSϫUЂWЄYЉ[Л]Н_вaдcйeмgцiшkьmёoіqќsўuѡwѱyѹ{ѽ}ѿҁ҃҅҇҉ҋҍҏґhtgg|²gjqn²fuvkh h ¡g¡²p¢£v£¤k¤¥i¥¦j¦§v§¨g¨²p©ªiª«q«¬"¬­t­®k®¯i¯°k°²f±±±±¢±©²³´n´µqµ¶q¶·u·¸g¸Æp¹ºtº»g»¼n¼½c½Æz¾¿i¿ÀqÀÁ"ÁÂnÂÃkÃÄoÄÆrÅ³Å¹Å¾ÆÇÈyÈÉjÉÊ{ÊËÞcÌÍcÍÞpÎÏvÏÐjÐÞgÑÒvÒÓjÓÔkÔÞuÕÖvÖ×j×ØcØÞvÙÚ{ÚÛqÛÜwÜÞtÝËÝÌÝÎÝÑÝÕÝÙÞ
ßàeàáwáâtâãtãägäåpåævæçèkèépéê"êëuëìnìíqíîyîï"ïðoðñqñòvòókóôqôļpõöxö÷g÷øtøù{ùú"úûhûücüýuýļvþÿhÿĀcĀāuāļvĂăpăĄqĄątąĆoĆćcćĈnĈĉnĉļ{ĊċxċČgČčtčĎ{Ďď"ďĐsĐđwđĒkĒēeēĔmĔĕnĕļ{ĖėsėĘwĘękęĚeĚěměĜnĜļ{ĝĞxĞğgğĠtĠġ{ġĢ"ĢģuģĤnĤĥqĥĦyĦħnħļ{ĨĩuĩĪnĪīqīĬyĬĭnĭļ{ĮįxįİgİıtıĲ{Ĳĳ"ĳĴuĴĵnĵĶqĶļyķĸuĸĹnĹĺqĺļyĻçĻõĻþĻĂĻĊĻĖĻĝĻĨĻĮĻķļĽľgľĿcĿštŀŁgŁł{łšgŃńgńŅ{ŅņgņšuŇňhňŉkŉŊpŊŋiŋŌgŌštōŎhŎŏqŏŐqŐšvőŒjŒœcœŔpŔšfŕŖjŖŗgŗŘgŘšnřŚpŚśqśŜuŜšgŝŞvŞşqşšgŠĽŠŀŠŃŠŇŠōŠőŠŕŠřŠŝšŢţcţŤtŤťgťŦŧcŧŨuŨũŪqŪūnūűfŬŭvŭŮcŮůnůűnŰũŰŬűŲƇz|ųŴjŴŵqŵŶtŶŷkŷŸ|ŸŹqŹźpźŻvŻżcżƇnŽžxžſgſƀtƀƁvƁƂkƂƃeƃƄcƄƇnƅƇƆŲƆųƆŽƆƅƇƈƉdƉƊgƊƋeƋƌqƌƍoƍƑgƎƏdƏƑgƐƈƐƎƑƒƓeƓƔqƔƕpƕƖhƖƗkƗƘiƘƙwƙƚtƚƛcƛƜvƜƝkƝƞqƞƟpƟƠơnơƢqƢƣyƣƤgƤƫtƥƦwƦƧrƧƨrƨƩgƩƫtƪƠƪƥƫƬƭfƭƮqƮ ƯưiưƱqƱƲcƲƳnƳƼuƴƵvƵƶcƶƷtƷƸiƸƹgƹƺvƺƼuƻƯƻƴƼ"ƽƾjƾƿgƿǀnǀǁnǁǉqǂǃjǃǄkǄǅiǅǉjǆǇjǇǉkǈƽǈǂǈǆǉ$ǊǋjǋǌcǌǍxǍǓgǎǏyǏǐgǐǑcǑǓtǒǊǒǎǓ&ǔǕjǕǖqǖǗnǗǘfǘ(ǙǚjǚǛqǛǜyǜ*ǝǞkǞǟpǟǠkǠǡvǡǢkǢǣcǣǤnǤǥkǥǦ|Ǧǧgǧ,ǨǩkǩǪ"ǪǫuǫǬcǬǭ{ǭ.Ǯǯkǯǰuǰ0Ǳǲkǲǳvǳ2ǴǵvǵǶgǶǷnǷǸnǸǹ"ǹǺoǺȈgǻǼfǼǽgǽǾuǾǿeǿȀtȀȁkȁȂdȂȈgȃȄnȄȅkȅȆuȆȈvȇǴȇǻȇȃȈ4ȉȊcȊȋtȋșoȌȍdȍȎcȎȏeȏșmȐȑnȑȒgȒșiȓȔvȔȕqȕȖtȖȗuȗșqȘȉȘȌȘȐȘȓș6ȚțnțȜkȜȝoȝȞkȞȟvȟȠuȠ8ȡȢoȢȣgȣȤcȤȥpȥȦuȦ:ȧȨcȨȩiȩɐgȪȫeȫȬcȬȭfȭȮgȮȯpȯȰeȰɐgȱȲeȲȳ{ȳȴeȴȵnȵȶgȶȷ"ȷȸvȸȹkȹȺoȺɐgȻȼfȼȽwȽȾvȾȿ{ȿɀ"ɀɁeɁɂ{ɂɃeɃɄnɄɐgɅɆjɆɇgɇɈkɈɉiɉɊjɊɐvɋɌpɌɍcɍɎoɎɐgɏȧɏȪɏȱɏȻɏɅɏɋɐ<ɑɒoɒɓkɓɔvɔɕvɕɖgɖɗpɗɘuɘ>əɚfɚɛgɛɜxɜɝkɝɞeɞɟgɟɭuɠɡlɡɢqɢɣkɣɤpɤɥvɥɭuɦɧoɧɨqɨɩvɩɪqɪɫtɫɭuɬəɬɠɬɦɭ@ɮɯfɯɰgɰɱxɱɲkɲɳeɳɿgɴɵlɵɶqɶɷkɷɸpɸɿvɹɺoɺɻqɻɼvɼɽqɽɿtɾɮɾɴɾɹɿBʀʁdʁʂgʂʃpʃʏfʄʅiʅʏqʆʇoʇʈqʈʉxʉʏgʊʋvʋʌwʌʍtʍʏpʎʀʎʄʎʆʎʊʏDʐʑqʑʖhʒʓhʓʔqʔʖtʕʐʕʒʖFʗʘqʘʙhʙʚhʚHʛʜqʜʝpʝJʞʟcʟʠpʠʡmʡʢnʢ˛gʣʤcʤʥtʥ˛oʦʧgʧʨnʨʩdʩʪqʪ˛yʫʬjʬʭgʭʮcʮ˛fʯʰjʰʱkʱ˛rʲʳvʳʴjʴʵkʵʶiʶ˛jʷʸmʸʹpʹʺgʺ˛gʻʼpʼʽgʽʾeʾ˛mʿˀuˀˁjˁ˂q˂˃w˃˄n˄˅f˅ˆgˆ˛tˇˈeˈˉjˉˊgˊˋuˋ˛vˌˍdˍˎwˎˏuˏ˛vːˑcˑ˒d˒˓f˓˔q˔˕o˕˖g˖˛p˗˘c˘˙d˙˛u˚ʞ˚ʣ˚ʦ˚ʫ˚ʯ˚ʲ˚ʷ˚ʻ˚ʿ˚ˇ˚ˌ˚ː˚˗˛L˜˝r˝˞q˞˟u˟ˠgˠNˡˢkˢˣfˣͤuˤ˥r˥˦q˦˧u˧˨k˨˩v˩˪k˪˫q˫ˬpˬͤu˭ˮqˮ˯h˯˰h˰˱u˱˲g˲˳v˳ͤu˴˵o˵˶k˶˷p˷˸k˸˹o˹˺w˺˻o˻˼"˼˽c˽˾p˾˿i˿̀n̀́g́ͤu̂̃o̃̄c̄̅z̅̆k̆̇o̇̈w̈̉o̉̊"̊̋c̋̌p̌̍i̍̎n̎̏g̏ͤu̐̑c̑̒p̒̓i̓̔n̔̕gͤ̕u̖̗o̗̘q̘̙v̙̚q̛̚t̛̜"̜̝v̝̞{̞̟r̟̠g̠ͤu̡̢q̢̣t̣̤k̤̥g̥̦p̧̦v̧̨c̨̩v̩̪k̪̫q̫̬p̬ͤu̭̮u̮̯r̯̰g̰̱g̱̲f̲ͤu̴̳u̴̵v̵̶c̶̷v̷̸g̸ͤu̹̺v̺̻q̻̼t̼̽s̽̾w̾̿g̿ͤu̀́n́͂q͂̓c̓̈́f̈́ͤu͆ͅv͇͆g͇͈o͈͉r͉͊g͊͋t͋͌c͍͌v͍͎w͎͏t͏͐g͐ͤu͑͒x͓͒q͓͔n͔͕v͕͖c͖͗i͗͘gͤ͘u͙͚x͚͛g͛͜n͜͝q͝͞e͟͞k͟͠v͠͡k͢͡gͤ͢uͣˡͣˤͣ˭ͣ˴ͣ̂ͣ̐̖ͣ̡ͣ̭ͣ̳ͣ̹ͣͣ̀ͣͅͣ͑͙ͣͤPͥͦkͦϪfͧͨrͨͩqͩͪuͪͫkͫͬvͬͭkͭͮqͮϪpͯͰqͰͱhͱͲhͲͳuͳʹgʹϪv͵ͶoͶͷkͷ͸p͸͹"͹ͺcͺͻpͻͼiͼͽnͽϪg;ͿoͿ΀c΀΁z΁΂"΂΃c΃΄p΄΅i΅ΆnΆϪg·ΈoΈΉkΉΊpΊ΋k΋ΌoΌ΍w΍ΎoΎΏ"ΏΐcΐΑpΑΒiΒΓnΓϪgΔΕoΕΖcΖΗzΗΘkΘΙoΙΚwΚΛoΛΜ"ΜΝcΝΞpΞΟiΟΠnΠϪgΡ΢c΢ΣpΣΤiΤΥnΥϪgΦΧoΧΨqΨΩvΩΪqΪΫtΫά"άέvέή{ήίrίϪgΰαqαβtβγkγδgδεpεζvζηcηθvθιkικqκϪpλμuμνrνξgξοgοϪfπρuρςvςσcστvτϪgυφvφχqχψtψωsωϊwϊϪgϋόnόύqύώcώϪfϏϐvϐϑgϑϒoϒϓrϓϔgϔϕtϕϖcϖϗvϗϘwϘϙtϙϪgϚϛxϛϜqϜϝnϝϞvϞϟcϟϠiϠϪgϡϢxϢϣgϣϤnϤϥqϥϦeϦϧkϧϨvϨϪ{ϩͥϩͧϩͯϩ͵ϩ;ϩ·ϩΔϩΡϩΦϩΰϩλϩπϩυϩϋϩϏϩϚϩϡϪRϫϬtϬϭgϭϮuϮϯgϯϰvϰTϱϲdϲϳgϳϴtϴЃvϵ϶d϶ϷwϷϸtϸЃvϹϺpϺϻqϻЃyϼϽrϽϾnϾϿgϿЀcЀЁuЁЃgЂϱЂϵЂϹЂϼЃVЄЅuЅІcІЇxЇЈgЈXЉЊuЊЋgЋЌvЌZЍЎnЎЏgЏАhАМvБВtВГkГДiДЕjЕМvЖЗqЗИvИЙjЙКgКМtЛЍЛБЛЖМ\НОuОПvПРtРСcСТkТУiУФjФХvХЦgЦЧpЧ^ШЩcЩЪuЪЫuЫЬwЬЭoЭгgЮЯvЯаcабmбгgвШвЮг`деvежjжзgзиpиbйкvклqлdмнfноgопiпрtрсgстgтуuуfфчw<хчu;цфцхчhшщ{щъqъыwыjьэyэюjюяcяѐvѐlёђyђѓjѓєgєѕpѕnіїyїјjјљgљњtњћgћpќѝ.ѝrўџ<џtѠѢ@ѡѠѡѢѢѦѣѥ}?ѤѣѥѨѦѤѦѧѧѩѨѦѩѭBѪѬ}?ѫѪѬѯѭѫѭѮѮvѯѭѰѲ@ѱѰѱѲѲѴѳѵ}?ѴѳѵѶѶѴѶѷѷxѸѺ{>ѹѸѺѻѻѹѻѼѼzѽѾ	Ѿ|ѿҀ	Ҁ~ҁ҂/҂҃҄?҄҅҆0҆҇҈1҈҉ҊaҊҋҌ+ҌҍҎ*ҎҏҐ$ҐґҒ)Ғ#±ÅÝĻŠŰƆƐƪƻǈǒȇȘɏɬɾʎʕ˚ͣϩЂЛвцѡѦѭѱѶѻ"""
            get() = Companion.field
        val _ATN: ATN = ATNDeserializer().deserialize(serializedATN.toCharArray())

        init {
            _decisionToDFA = arrayOfNulls<DFA>(_ATN.getNumberOfDecisions())
            for (i in 0 until _ATN.getNumberOfDecisions()) {
                _decisionToDFA[i] = DFA(_ATN.getDecisionState(i), i)
            }
        }
    }
}