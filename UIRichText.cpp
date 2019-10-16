/****************************************************************************
 Copyright (c) 2013 cocos2d-x.org
 
 http://www.cocos2d-x.org
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/

#include "UIRichText.h"
#include "platform/CCFileUtils.h"
#include "2d/CCLabel.h"
#include "2d/CCSprite.h"
#include "base/ccUTF8.h"
#include "ui/UIHelper.h"

NS_CC_BEGIN

namespace ui {
    
bool RichElement::init(int tag, const Color3B &color, GLubyte opacity)
{
    _tag = tag;
    _color = color;
    _opacity = opacity;
    return true;
}
    
    
RichElementText* RichElementText::create(int tag, const Color3B &color, GLubyte opacity, const std::string& text, const std::string& fontName, float fontSize)
{
    RichElementText* element = new (std::nothrow) RichElementText();
    if (element && element->init(tag, color, opacity, text, fontName, fontSize))
    {
        element->autorelease();
        return element;
    }
    CC_SAFE_DELETE(element);
    return nullptr;
}
    
bool RichElementText::init(int tag, const Color3B &color, GLubyte opacity, const std::string& text, const std::string& fontName, float fontSize)
{
    if (RichElement::init(tag, color, opacity))
    {
        _text = text;
        _fontName = fontName;
        _fontSize = fontSize;
        return true;
    }
    return false;
}

RichElementImage* RichElementImage::create(int tag, const Color3B &color, GLubyte opacity, const std::string& filePath)
{
    RichElementImage* element = new (std::nothrow) RichElementImage();
    if (element && element->init(tag, color, opacity, filePath))
    {
        element->autorelease();
        return element;
    }
    CC_SAFE_DELETE(element);
    return nullptr;
}
    
bool RichElementImage::init(int tag, const Color3B &color, GLubyte opacity, const std::string& filePath)
{
    if (RichElement::init(tag, color, opacity))
    {
        _filePath = filePath;
        return true;
    }
    return false;
}

RichElementCustomNode* RichElementCustomNode::create(int tag, const Color3B &color, GLubyte opacity, cocos2d::Node *customNode)
{
    RichElementCustomNode* element = new (std::nothrow) RichElementCustomNode();
    if (element && element->init(tag, color, opacity, customNode))
    {
        element->autorelease();
        return element;
    }
    CC_SAFE_DELETE(element);
    return nullptr;
}
    
bool RichElementCustomNode::init(int tag, const Color3B &color, GLubyte opacity, cocos2d::Node *customNode)
{
    if (RichElement::init(tag, color, opacity))
    {
        _customNode = customNode;
        _customNode->retain();
        return true;
    }
    return false;
}
    
RichElementNewLine* RichElementNewLine::create(int tag, const Color3B& color, GLubyte opacity)
{
    RichElementNewLine* element = new (std::nothrow) RichElementNewLine();
    if (element && element->init(tag, color, opacity))
    {
        element->autorelease();
        return element;
    }
    CC_SAFE_DELETE(element);
    return nullptr;
}
    
RichText::RichText():
_formatTextDirty(true),
_leftSpaceWidth(0.0f),
_verticalSpace(0.0f),
m_fLimitMaxWidth(0.0f)
{
    
}
    
RichText::~RichText()
{
    _richElements.clear();
}
    
RichText* RichText::create()
{
    RichText* widget = new (std::nothrow) RichText();
    if (widget && widget->init())
    {
        widget->autorelease();
        return widget;
    }
    CC_SAFE_DELETE(widget);
    return nullptr;
}
    
bool RichText::init()
{
    if (Widget::init())
    {
        return true;
    }
    return false;
}
    
void RichText::initRenderer()
{
}

void RichText::insertElement(RichElement *element, int index)
{
    _richElements.insert(index, element);
    _formatTextDirty = true;
}
    
void RichText::pushBackElement(RichElement *element)
{
    _richElements.pushBack(element);
    _formatTextDirty = true;
}
    
void RichText::removeElement(int index)
{
    _richElements.erase(index);
    _formatTextDirty = true;
}
    
void RichText::removeElement(RichElement *element)
{
    _richElements.eraseObject(element);
    _formatTextDirty = true;
}
    
void RichText::formatText()
{
    if (_formatTextDirty)
    {
        this->removeAllProtectedChildren();
        _elementRenders.clear();
        if (_ignoreSize)
        {
            addNewLine();
            for (ssize_t i=0; i<_richElements.size(); i++)
            {
                RichElement* element = _richElements.at(i);
                Node* elementRenderer = nullptr;
                switch (element->_type)
                {
                    case RichElement::Type::TEXT:
                    {
                        RichElementText* elmtText = static_cast<RichElementText*>(element);
                        if (FileUtils::getInstance()->isFileExist(elmtText->_fontName))
                        {
                            elementRenderer = Label::createWithTTF(elmtText->_text.c_str(), elmtText->_fontName, elmtText->_fontSize);
                        }
                        else
                        {
                            elementRenderer = Label::createWithSystemFont(elmtText->_text.c_str(), elmtText->_fontName, elmtText->_fontSize);
                        }
                        break;
                    }
                    case RichElement::Type::IMAGE:
                    {
                        RichElementImage* elmtImage = static_cast<RichElementImage*>(element);
                        elementRenderer = Sprite::create(elmtImage->_filePath.c_str());
                        break;
                    }
                    case RichElement::Type::CUSTOM:
                    {
                        RichElementCustomNode* elmtCustom = static_cast<RichElementCustomNode*>(element);
                        elementRenderer = elmtCustom->_customNode;
                        break;
                    }
                    case RichElement::Type::NEWLINE:
                    {
                        addNewLine();
                        break;
                    }
                    default:
                        break;
                }
                elementRenderer->setColor(element->_color);
                elementRenderer->setOpacity(element->_opacity);
                pushToContainer(elementRenderer);
            }
        }
        else
        {
            addNewLine();
            for (ssize_t i=0; i<_richElements.size(); i++)
            {
                
                RichElement* element = static_cast<RichElement*>(_richElements.at(i));
                switch (element->_type)
                {
                    case RichElement::Type::TEXT:
                    {
                        RichElementText* elmtText = static_cast<RichElementText*>(element);
                        handleTextRenderer(elmtText->_text.c_str(), elmtText->_fontName.c_str(), elmtText->_fontSize, elmtText->_color, elmtText->_opacity);
                        break;
                    }
                    case RichElement::Type::IMAGE:
                    {
                        RichElementImage* elmtImage = static_cast<RichElementImage*>(element);
                        handleImageRenderer(elmtImage->_filePath.c_str(), elmtImage->_color, elmtImage->_opacity);
                        break;
                    }
                    case RichElement::Type::CUSTOM:
                    {
                        RichElementCustomNode* elmtCustom = static_cast<RichElementCustomNode*>(element);
                        handleCustomRenderer(elmtCustom->_customNode);
                        break;
                    }
                    case RichElement::Type::NEWLINE:
                    {
                        addNewLine();
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        formarRenderers();
        _formatTextDirty = false;
    }
}

void RichText::selectMaxValue(int& outValue, int& inputValue)
{
	outValue = (outValue > inputValue) ? outValue : inputValue;
}

void RichText::selectMinValue(int& outValue, int& inputValue)
{
	//小于0忽略
	if (inputValue > 0)
	{
		outValue = (outValue < inputValue) ? outValue : inputValue;
	}
}

void RichText::adjustStringComping(int& leftLength, float& leftWidth, int findNewLinePos, std::string& leftStr, std::string& curText, Label* textRenderer)
{
	textRenderer->setString(leftStr);
	float newTextWidth = textRenderer->getContentSize().width;
	bool isOver = (newTextWidth + 12.0f) > m_fLimitMaxWidth;

	//取leftLength该位置的字符
	std::string endStr = curText.substr(leftLength - 1, 1);
	if (endStr != " ")
	{
		endStr = curText.substr(leftLength, 1);
	}
	//从当前已经截断的字符串的末位分别查找前后最近的空格字符的位置
	int nextSpaceCharPos = (endStr == " ") ? leftLength + 1 : curText.find_first_of(" ", leftLength);
	int preSpaceCharPos = (endStr == " ") ? curText.find_last_of(" ", leftLength - 1) : curText.find_last_of(" ", leftLength);

	//英文逗号
	int nextCommaCharPos = (endStr == ",") ? leftLength + 1 : curText.find_first_of(",", leftLength);
	int preCommaCharPos = (endStr == ",") ? curText.find_last_of(",", leftLength - 1) : curText.find_last_of(",", leftLength);

	//英文句号
	int nextDotCharPos = (endStr == ".") ? leftLength + 1 : curText.find_first_of(".", leftLength);
	int preDotCharPos = (endStr == ".") ? curText.find_last_of(".", leftLength - 1) : curText.find_last_of(".", leftLength);

	//英文问号
	int nextQuestionCharPos = (endStr == "?") ? leftLength + 1 : curText.find_first_of("?", leftLength);
	int preQuestionCharPos = (endStr == "?") ? curText.find_last_of("?", leftLength - 1) : curText.find_last_of("?", leftLength);

	//英文分号
	int nextSemicolonCharPos = (endStr == ";") ? leftLength + 1 : curText.find_first_of(";", leftLength);
	int preSemicolonCharPos = (endStr == ";") ? curText.find_last_of(";", leftLength - 1) : curText.find_last_of(";", leftLength);

	//英文感叹号
	int nextExclamationMarkCharPos = (endStr == "!") ? leftLength + 1 : curText.find_first_of("!", leftLength);
	int preExclamationMarkCharPos = (endStr == "!") ? curText.find_last_of("!", leftLength - 1) : curText.find_last_of("!", leftLength);

	//英文等于号
	int nextEqualMarkCharPos = (endStr == "=") ? leftLength + 1 : curText.find_first_of("=", leftLength);
	int preEqualMarkCarPos = (endStr == "=") ? curText.find_last_of("=", leftLength - 1) : curText.find_last_of("=", leftLength);

	//英文小括号
	int nextParenthesesCharPos = (endStr == ")") ? leftLength + 1 : curText.find_first_of(")", leftLength);
	int preParenthesesCharPos = curText.find_last_of("(", leftLength);


	//英文中括号
	int nextMiddleMarkCharPos = (endStr == "]") ? leftLength + 1 : curText.find_first_of("]", leftLength);
	int preMiddleMarkCharPos = curText.find_last_of("[", leftLength);

	//英文冒号
	int nextColonCharPos = (endStr == ":") ? leftLength + 1 : curText.find_first_of(":", leftLength);
	int preColonCharPos = (endStr == ":") ? curText.find_last_of(":", leftLength - 1) : curText.find_last_of(":", leftLength);

	//比较所有前置符号谁更大
	int preMaxMark = preSpaceCharPos;
	selectMaxValue(preMaxMark, preCommaCharPos);
	selectMaxValue(preMaxMark, preDotCharPos);
	selectMaxValue(preMaxMark, preQuestionCharPos);
	selectMaxValue(preMaxMark, preSemicolonCharPos);
	selectMaxValue(preMaxMark, preExclamationMarkCharPos);
	selectMaxValue(preMaxMark, preEqualMarkCarPos);
	selectMaxValue(preMaxMark, preParenthesesCharPos);
	selectMaxValue(preMaxMark, preMiddleMarkCharPos);
	selectMaxValue(preMaxMark, preColonCharPos);

	if (findNewLinePos > 0)
	{
		nextSpaceCharPos = ((nextSpaceCharPos > findNewLinePos) || (nextSpaceCharPos == -1)) ? 
		findNewLinePos : nextSpaceCharPos;
	}
	//比较所有后置符号谁更小
	int nextMinMark = nextSpaceCharPos;
	
	selectMinValue(nextMinMark, nextCommaCharPos);
	selectMinValue(nextMinMark, nextDotCharPos);
	selectMinValue(nextMinMark, nextQuestionCharPos);
	selectMinValue(nextMinMark, nextSemicolonCharPos);
	selectMinValue(nextMinMark, nextExclamationMarkCharPos);
	selectMinValue(nextMinMark, nextEqualMarkCharPos);
	selectMinValue(nextMinMark, nextParenthesesCharPos);
	selectMinValue(nextMinMark, nextMiddleMarkCharPos);
	selectMinValue(nextMinMark, nextColonCharPos);

	if (nextMinMark != std::string::npos && preMaxMark != std::string::npos)
	{
		if (nextMinMark <= preMaxMark)
		{
			return;
		}
		//说明被截断位置的后续字符跟前面字符是一个整体
		if (nextMinMark - leftLength > 0)
		{
			leftLength -= (leftLength - preMaxMark - 1);
			leftStr = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
			textRenderer->setString(leftStr);
			leftWidth = textRenderer->getContentSize().width;
		}
		
		if (!isOver)
		{
			return;
		}

		//超框处理
		if ((leftWidth + 12.0f) >= m_fLimitMaxWidth && m_fLimitMaxWidth > 0.0f)
		{
			leftLength -= (nextMinMark - preMaxMark - 1);
			leftStr = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
			textRenderer->setString(leftStr);
			leftWidth = textRenderer->getContentSize().width;
		}
	}
}
    
void RichText::handleTextRenderer(const std::string& text, const std::string& fontName, float fontSize, const Color3B &color, GLubyte opacity)
{
    auto fileExist = FileUtils::getInstance()->isFileExist(fontName);
    Label* textRenderer = nullptr;
    if (fileExist)
    {
        textRenderer = Label::createWithTTF(text, fontName, fontSize);
    } 
    else
    {
        textRenderer = Label::createWithSystemFont(text, fontName, fontSize);
    }
    float textRendererWidth = textRenderer->getContentSize().width;
    _leftSpaceWidth -= textRendererWidth;
	// add by wangzhen
	int findNewLinePos = text.find("\n");
	if (std::string::npos != findNewLinePos) {
		// 计算\n在utf中第几个字符
		std::string strTemp = text.substr(0, findNewLinePos);
		findNewLinePos = StringUtils::getCharacterCountInUTF8String(strTemp);
	}
	
    if (_leftSpaceWidth < 0.0f)
    {
        float overstepPercent = (-_leftSpaceWidth) / textRendererWidth;
        std::string curText = text;
        size_t stringLength = StringUtils::getCharacterCountInUTF8String(text);
        int leftLength = stringLength * (1.0f - overstepPercent);
        
        // The adjustment of the new line position
        auto originalLeftSpaceWidth = _leftSpaceWidth + textRendererWidth;
        auto leftStr = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
        textRenderer->setString(leftStr);
        auto leftWidth = textRenderer->getContentSize().width;
        if (originalLeftSpaceWidth < leftWidth) {
            // Have protruding
            for (;;) {
                leftLength--;
                leftStr = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
                textRenderer->setString(leftStr);
                leftWidth = textRenderer->getContentSize().width;

				//================add by wangzhen=======
				if (leftWidth <= originalLeftSpaceWidth || leftLength <= 0)
				{
					//检测是否是完整的英文单词
					adjustStringComping(leftLength, leftWidth, findNewLinePos, leftStr, curText, textRenderer);
					break;
				}
				
				//=======================================
                /*if (leftWidth <= originalLeftSpaceWidth) {
                    break;
                }
                else if (leftLength <= 0) {
                    break;
                }*/
            }
        }
        else if (leftWidth < originalLeftSpaceWidth) {
            // A wide margin
            for (;;) {
                leftLength++;
                leftStr = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
                textRenderer->setString(leftStr);
                leftWidth = textRenderer->getContentSize().width;

				//================add by wangzhen==========================================
				if (originalLeftSpaceWidth < leftWidth || stringLength <= leftLength)
				{
					//检测是否是完整的英文单词
					adjustStringComping(leftLength, leftWidth, findNewLinePos, leftStr, curText, textRenderer);
					break;
				}

				if ((stringLength >> 1) <= leftLength)
				{
					break;
				}
				//===============================================================================================

                /*if (originalLeftSpaceWidth < leftWidth) {
                    leftLength--;
                    break;
                }
                else if (stringLength <= leftLength) {
                    break;
                }*/
            }
        }
		else
		{
			//检测是否是完整的英文单词
			adjustStringComping(leftLength, leftWidth, findNewLinePos, leftStr, curText, textRenderer);
		}
        //The minimum cut length is 1, otherwise will cause the infinite loop.
        //if (0 == leftLength) leftLength = 1;
		// 假如有\n要提前换行  modify by zhong
		bool bFindNewLine = false;
		if (std::string::npos != findNewLinePos && findNewLinePos < leftLength) {
			leftLength = findNewLinePos;
			bFindNewLine = true;
		}
		std::string leftWords;
		if (leftLength > 0)
		{
			leftWords = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
			//================add by wangzhen=============
			adjustStringComping(leftLength, leftWidth, findNewLinePos, leftWords, curText, textRenderer);
			//===========================================
		}
			
		Label* leftRenderer = nullptr;
		if (leftLength > 0) {
			if (fileExist) {
				leftRenderer = Label::createWithTTF(Helper::getSubStringOfUTF8String(leftWords, 0, leftLength), fontName, fontSize);
			}
			else {
				leftRenderer = Label::createWithSystemFont(Helper::getSubStringOfUTF8String(leftWords, 0, leftLength), fontName, fontSize);
			}
		}
		else {
		//	leftRenderer = Label::create();
			//add wangzhen 多个换行符不能忽略高度
			if (fileExist)
			{
				leftRenderer = Label::createWithTTF(" ", fontName, fontSize);
			}
			else
			{
				leftRenderer = Label::createWithSystemFont(" ", fontName, fontSize);
			}
		}

		if (leftRenderer) {
			leftRenderer->setColor(color);
			leftRenderer->setOpacity(opacity);
			pushToContainer(leftRenderer);
		}

		addNewLine();
		// 跳过\n
		if (bFindNewLine) {
			++ leftLength;
		}
		std::string cutWords = Helper::getSubStringOfUTF8String(curText, leftLength, stringLength - leftLength);
		handleTextRenderer(cutWords.c_str(), fontName, fontSize, color, opacity);
    }
    else
    {
		// 假如有\n要提前换行  modify by zhong
		if (std::string::npos != findNewLinePos) {
			size_t stringLength = StringUtils::getCharacterCountInUTF8String(text);
			std::string curText = text;
			std::string leftWords;
			int leftLength = findNewLinePos;
			if (leftLength > 0)
			{
				leftWords = Helper::getSubStringOfUTF8String(curText, 0, leftLength);
				//================add by wangzhen=======
				float leftWidth = 0.0f;
				adjustStringComping(leftLength, leftWidth, findNewLinePos, leftWords, curText, textRenderer);
			}
				
			Label* leftRenderer = nullptr;
			if (leftLength > 0)
			{
				if (fileExist)
				{
					leftRenderer = Label::createWithTTF(Helper::getSubStringOfUTF8String(leftWords, 0, leftLength), fontName, fontSize);
				}
				else
				{
					leftRenderer = Label::createWithSystemFont(Helper::getSubStringOfUTF8String(leftWords, 0, leftLength), fontName, fontSize);
				}
			}
			else
			{
				//leftRenderer = Label::create();
				//add wangzhen 多个换行符不能忽略高度
				if (fileExist)
				{
					leftRenderer = Label::createWithTTF(" ", fontName, fontSize);
				}
				else
				{
					leftRenderer = Label::createWithSystemFont(" ", fontName, fontSize);
				}
			}

			if (leftRenderer)
			{
				leftRenderer->setColor(color);
				leftRenderer->setOpacity(opacity);
				pushToContainer(leftRenderer);
			}

			addNewLine();
			// 跳过\n
			++leftLength;
			std::string cutWords = Helper::getSubStringOfUTF8String(curText, leftLength, stringLength - leftLength);
			handleTextRenderer(cutWords.c_str(), fontName, fontSize, color, opacity);
		} else {

			textRenderer->setColor(color);
			textRenderer->setOpacity(opacity);
			pushToContainer(textRenderer);
		}
    }
}
    
void RichText::handleImageRenderer(const std::string& fileParh, const Color3B &color, GLubyte opacity)
{
    Sprite* imageRenderer = Sprite::create(fileParh);
    handleCustomRenderer(imageRenderer);
}
    
void RichText::handleCustomRenderer(cocos2d::Node *renderer)
{
    Size imgSize = renderer->getContentSize();
    _leftSpaceWidth -= imgSize.width;
    if (_leftSpaceWidth < 0.0f)
    {
        addNewLine();
        pushToContainer(renderer);
        _leftSpaceWidth -= imgSize.width;
    }
    else
    {
        pushToContainer(renderer);
    }
}
    
void RichText::addNewLine()
{
    _leftSpaceWidth = _customSize.width;
    _elementRenders.push_back(new Vector<Node*>());
}
    
void RichText::formarRenderers()
{
    if (_ignoreSize)
    {
        float newContentSizeWidth = 0.0f;
        float newContentSizeHeight = 0.0f;
        
        Vector<Node*>* row = (_elementRenders[0]);
        float nextPosX = 0.0f;
        for (ssize_t j=0; j<row->size(); j++)
        {
            Node* l = row->at(j);
            l->setAnchorPoint(Vec2::ZERO);
            l->setPosition(nextPosX, 0.0f);
            this->addProtectedChild(l, 1);
            Size iSize = l->getContentSize();
            newContentSizeWidth += iSize.width;
            newContentSizeHeight = MAX(newContentSizeHeight, iSize.height);
            nextPosX += iSize.width;
        }
        this->setContentSize(Size(newContentSizeWidth, newContentSizeHeight));
    }
    else
    {
        float newContentSizeHeight = 0.0f;
        float *maxHeights = new float[_elementRenders.size()];
        
        for (size_t i=0; i<_elementRenders.size(); i++)
        {
			// add by wangzhen
			if (0 != i) {
				newContentSizeHeight += _verticalSpace;
			}
            Vector<Node*>* row = (_elementRenders[i]);
            float maxHeight = 0.0f;
            for (ssize_t j=0; j<row->size(); j++)
            {
                Node* l = row->at(j);
                maxHeight = MAX(l->getContentSize().height, maxHeight);
            }
            maxHeights[i] = maxHeight;
            newContentSizeHeight += maxHeights[i];
        }
		//====add wangzhen====
		_customSize.height = newContentSizeHeight;
		float realWidth = 0;
		//================
        float nextPosY = _customSize.height;
		
        for (size_t i=0; i<_elementRenders.size(); i++)
        {
            Vector<Node*>* row = (_elementRenders[i]);
            float nextPosX = 0.0f;
			// modify by zhong
			float newlineHeight = maxHeights[i];
			if (0 != i) {
				newlineHeight += _verticalSpace;
			}
			nextPosY -= newlineHeight;
            
            for (ssize_t j=0; j<row->size(); j++)
            {
                Node* l = row->at(j);
                l->setAnchorPoint(Vec2::ZERO);
                l->setPosition(nextPosX, nextPosY);
                this->addProtectedChild(l, 1);
                nextPosX += l->getContentSize().width;
            }
			//====add wangzhen====
			if (realWidth < nextPosX)
			{
				realWidth = nextPosX;
			}
			//================
        }
		//=====add wangzhen======
		m_realRichSize.width = realWidth;
		m_realRichSize.height = _customSize.height;
		//============================
        delete [] maxHeights;
    }
    
    size_t length = _elementRenders.size();
    for (size_t i = 0; i<length; i++)
	{
        Vector<Node*>* l = _elementRenders[i];
        l->clear();
        delete l;
	}    
    _elementRenders.clear();
    
    if (_ignoreSize)
    {
        Size s = getVirtualRendererSize();
        this->setContentSize(s);
    }
    else
    {
        this->setContentSize(_customSize);
    }
    updateContentSizeWithTextureSize(_contentSize);
}
    
void RichText::adaptRenderers()
{
    this->formatText();
}
    
void RichText::pushToContainer(cocos2d::Node *renderer)
{
    if (_elementRenders.size() <= 0)
    {
        return;
    }
    _elementRenders[_elementRenders.size()-1]->pushBack(renderer);
}
    
void RichText::setVerticalSpace(float space)
{
    _verticalSpace = space;
}

void RichText::ignoreContentAdaptWithSize(bool ignore)
{
    if (_ignoreSize != ignore)
    {
        _formatTextDirty = true;
        Widget::ignoreContentAdaptWithSize(ignore);
    }
}
    
std::string RichText::getDescription() const
{
    return "RichText";
}

Size RichText::getRealSize()
{
	return m_realRichSize;
}

void RichText::refreshRichText()
{
	_ignoreSize = false;
	_formatTextDirty = true;
	this->formatText();
}

}

NS_CC_END
